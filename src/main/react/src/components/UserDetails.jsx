import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRoleConfig } from '../context/RoleConfigContext';
import { useModules } from '../hooks/useModules';
import { getUser, createUser, updateUser } from '../api/roleManagerApi';
import Toast from './common/Toast';

export default function UserDetails({ isNew = false, isClone = false }) {
  const { username } = useParams();
  const navigate = useNavigate();
  const { currentUser, isAdmin } = useAuth();
  const { level1Types, roleDbCreator, roleDbSupervisor, roleSeparator } = useRoleConfig();
  const { publicModules, privateModules, loading: modulesLoading } = useModules();

  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    dbCreator: false,
    supervisorModules: [],
    permissions: [],
  });
  
  const [userData, setUserData] = useState(null);
  const [errors, setErrors] = useState([]);

  // Load existing user data
  useEffect(() => {
    if (isNew && !isClone) {
      setLoading(false);
      return;
    }

    if (username) {
      loadUser();
    }
  }, [username, isNew, isClone]);

  async function loadUser() {
    try {
      setLoading(true);
      const data = await getUser(username);
      setUserData(data);
      
      // Pre-fill form (don't pre-fill password for security)
      setFormData({
        username: isClone ? '' : data.username,
        password: '',
        confirmPassword: '',
        email: isClone ? '' : (data.email || ''),
        dbCreator: data.isDbCreator || false,
        supervisorModules: Array.from(data.supervisedModules || []),
        permissions: buildPermissionsFromAuthorities(data),
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function buildPermissionsFromAuthorities(data) {
    const permissions = [];
    
    // Extract from customRoles: { module: { entityType: { role: [entityIds] } } }
    if (data.customRoles) {
      Object.entries(data.customRoles).forEach(([module, byType]) => {
        Object.entries(byType).forEach(([entityType, byRole]) => {
          Object.entries(byRole).forEach(([role, entityIds]) => {
            entityIds.forEach(entityId => {
              permissions.push({ module, entityType, role, entityId: String(entityId) });
            });
          });
        });
      });
    }
    
    return permissions;
  }

  function handleInputChange(e) {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  }

  function handleSupervisorChange(module, checked) {
    setFormData(prev => ({
      ...prev,
      supervisorModules: checked
        ? [...prev.supervisorModules, module]
        : prev.supervisorModules.filter(m => m !== module),
    }));
  }

  function validateForm() {
    const newErrors = [];
    
    if (!formData.username.trim()) {
      newErrors.push('Username is required');
    }
    
    if ((isNew || isClone) && !formData.password) {
      newErrors.push('Password is required for new users');
    }
    
    if (formData.password && formData.password !== formData.confirmPassword) {
      newErrors.push('Passwords do not match');
    }
    
    if (formData.email && !isValidEmail(formData.email)) {
      newErrors.push('Invalid email address');
    }
    
    setErrors(newErrors);
    return newErrors.length === 0;
  }

  function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      setSaving(true);
      setError(null);
      
      const payload = {
        username: formData.username,
        email: formData.email || null,
        dbCreator: formData.dbCreator,
        supervisorModules: formData.supervisorModules,
        permissions: formData.permissions,
        cloning: isClone,
      };
      
      if (formData.password) {
        payload.password = formData.password;
      }

      if (isNew || isClone) {
        await createUser(payload);
        setToast({ type: 'success', message: 'User created successfully' });
      } else {
        await updateUser(username, payload);
        setToast({ type: 'success', message: 'User updated successfully' });
      }

      // Navigate back to user list after short delay
      setTimeout(() => navigate('/users'), 1500);
    } catch (err) {
      setError(err.message);
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  function renderModuleSelector(modules, isPublic) {
    if (!modules || modules.length === 0) return null;

    return (
      <div className="mb-3">
        <label className="form-label fw-medium">
          {isPublic ? 'Public' : 'Private'} Databases
        </label>
        <div className="row">
          {modules.map(module => (
            <div key={module.name} className="col-md-4 col-sm-6 mb-2">
              <div className="card h-100">
                <div className="card-body py-2 px-3">
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="fw-medium">{module.name}</span>
                    {(isAdmin || module.canSupervise) && (
                      <div className="form-check form-check-inline mb-0">
                        <input
                          type="checkbox"
                          className="form-check-input"
                          id={`supervisor-${module.name}`}
                          checked={formData.supervisorModules.includes(module.name)}
                          onChange={(e) => handleSupervisorChange(module.name, e.target.checked)}
                          disabled={userData?.isAdmin}
                        />
                        <label className="form-check-label small" htmlFor={`supervisor-${module.name}`}>
                          Supervisor
                        </label>
                      </div>
                    )}
                  </div>
                  
                  {/* Link to permission editor for each entity type */}
                  {level1Types.length > 0 && !formData.supervisorModules.includes(module.name) && (
                    <div className="mt-2">
                      {level1Types.map(entityType => (
                        <Link
                          key={entityType}
                          to={`/user/${encodeURIComponent(isNew || isClone ? formData.username : username)}/permissions/${encodeURIComponent(module.name)}/${entityType}`}
                          className="btn btn-sm btn-outline-secondary me-1 mb-1"
                          onClick={(e) => {
                            if (!formData.username && (isNew || isClone)) {
                              e.preventDefault();
                              setToast({ type: 'warning', message: 'Please enter a username first' });
                            }
                          }}
                        >
                          <i className="bi bi-key me-1"></i>
                          {entityType}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (loading || modulesLoading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (error && !userData) {
    return (
      <div className="alert alert-danger">
        <i className="bi bi-exclamation-triangle me-2"></i>
        {error}
        <Link to="/users" className="btn btn-secondary ms-3">Back to Users</Link>
      </div>
    );
  }

  const pageTitle = isNew ? 'Create New User' : isClone ? `Clone User: ${username}` : `Edit User: ${username}`;

  return (
    <div className="role-manager-container">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className={`bi ${isNew ? 'bi-person-plus' : isClone ? 'bi-copy' : 'bi-person-gear'} me-2`}></i>
          {pageTitle}
        </h2>
        <Link to="/users" className="btn btn-outline-secondary">
          <i className="bi bi-arrow-left me-1"></i>
          Back to Users
        </Link>
      </div>

      {/* Validation Errors */}
      {errors.length > 0 && (
        <div className="alert alert-danger">
          <i className="bi bi-exclamation-triangle me-2"></i>
          <ul className="mb-0">
            {errors.map((err, idx) => (
              <li key={idx}>{err}</li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Basic Info Section */}
        <div className="form-section">
          <h5 className="form-section-title">
            <i className="bi bi-person me-2"></i>
            Account Information
          </h5>
          
          <div className="row">
            <div className="col-md-6 mb-3">
              <label htmlFor="username" className="form-label">Username *</label>
              <input
                type="text"
                className="form-control"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                disabled={!isNew && !isClone}
                required
              />
            </div>
            
            <div className="col-md-6 mb-3">
              <label htmlFor="email" className="form-label">Email</label>
              <input
                type="email"
                className="form-control"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
              />
            </div>
          </div>

          <div className="row">
            <div className="col-md-6 mb-3">
              <label htmlFor="password" className="form-label">
                Password {(isNew || isClone) && '*'}
              </label>
              <input
                type="password"
                className="form-control"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                required={isNew || isClone}
                placeholder={!isNew && !isClone ? 'Leave blank to keep current' : ''}
              />
            </div>
            
            <div className="col-md-6 mb-3">
              <label htmlFor="confirmPassword" className="form-label">
                Confirm Password {(isNew || isClone) && '*'}
              </label>
              <input
                type="password"
                className="form-control"
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleInputChange}
                required={isNew || isClone}
              />
            </div>
          </div>

          {/* Read-only info for existing users */}
          {userData && (
            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Authentication Method</label>
                <input
                  type="text"
                  className="form-control"
                  value={userData.method}
                  disabled
                />
              </div>
            </div>
          )}
        </div>

        {/* Roles Section */}
        {isAdmin && !userData?.isAdmin && (
          <div className="form-section">
            <h5 className="form-section-title">
              <i className="bi bi-shield me-2"></i>
              Global Roles
            </h5>
            
            <div className="form-check">
              <input
                type="checkbox"
                className="form-check-input"
                id="dbCreator"
                name="dbCreator"
                checked={formData.dbCreator}
                onChange={handleInputChange}
              />
              <label className="form-check-label" htmlFor="dbCreator">
                Database Creator
                <small className="text-muted d-block">Can create new databases</small>
              </label>
            </div>
          </div>
        )}

        {/* Admin Notice */}
        {userData?.isAdmin && (
          <div className="alert alert-info">
            <i className="bi bi-info-circle me-2"></i>
            This user has administrator privileges. Permission settings are managed separately.
          </div>
        )}

        {/* Module Permissions Section */}
        {!userData?.isAdmin && (
          <div className="form-section">
            <h5 className="form-section-title">
              <i className="bi bi-database me-2"></i>
              Database Permissions
            </h5>
            
            <p className="text-muted small mb-3">
              Set this user as Supervisor for full access, or click on an entity type to manage specific permissions.
            </p>
            
            {renderModuleSelector(publicModules, true)}
            {renderModuleSelector(privateModules, false)}
            
            {publicModules.length === 0 && privateModules.length === 0 && (
              <p className="text-muted">No databases available for permission management.</p>
            )}
          </div>
        )}

        {/* Form Actions */}
        <div className="d-flex justify-content-end gap-2">
          <Link to="/users" className="btn btn-secondary">
            Cancel
          </Link>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Saving...
              </>
            ) : (
              <>
                <i className="bi bi-check-lg me-1"></i>
                {isNew || isClone ? 'Create User' : 'Save Changes'}
              </>
            )}
          </button>
        </div>
      </form>

      {/* Toast */}
      {toast && (
        <Toast 
          type={toast.type} 
          message={toast.message} 
          onClose={() => setToast(null)} 
        />
      )}
    </div>
  );
}
