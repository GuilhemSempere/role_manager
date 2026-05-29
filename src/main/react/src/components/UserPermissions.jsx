import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRoleConfig } from '../context/RoleConfigContext';
import { getPermissionFormData, addPermission, removePermission } from '../api/roleManagerApi';
import Toast from './common/Toast';

export default function UserPermissions() {
  const { username, module, entityType } = useParams();
  const navigate = useNavigate();
  const { isAdmin, supervisedModules, managedEntities } = useAuth();
  const { getLevel1Roles, roleSeparator, entityManagerRole } = useRoleConfig();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  const [formData, setFormData] = useState(null);
  const [selectedPermissions, setSelectedPermissions] = useState({});

  const roles = getLevel1Roles(entityType);

  useEffect(() => {
    loadData();
  }, [username, module, entityType]);

  async function loadData() {
    try {
      setLoading(true);
      const data = await getPermissionFormData(username, module, entityType);
      setFormData(data);
      
      // Build selected permissions map from user's authorities
      const selected = {};
      if (data.userAuthorities) {
        data.userAuthorities.forEach(auth => {
          const prefix = `${module}${roleSeparator}${entityType}${roleSeparator}`;
          if (auth.startsWith(prefix)) {
            const rest = auth.substring(prefix.length);
            const sepIdx = rest.indexOf(roleSeparator);
            if (sepIdx > 0) {
              const role = rest.substring(0, sepIdx);
              const entityId = rest.substring(sepIdx + 1);
              const key = `${entityId}:${role}`;
              selected[key] = true;
            }
          }
        });
      }
      setSelectedPermissions(selected);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function canManageEntity(entityId) {
    if (isAdmin) return true;
    if (supervisedModules.includes(module)) return true;
    
    // Check if current user is manager of this entity
    const moduleEntities = managedEntities[module];
    if (moduleEntities) {
      const typeEntities = moduleEntities[entityType];
      if (typeEntities) {
        return typeEntities.includes(entityId) || typeEntities.includes(String(entityId));
      }
    }
    return false;
  }

  function handlePermissionChange(entityId, role, checked) {
    const key = `${entityId}:${role}`;
    setSelectedPermissions(prev => ({
      ...prev,
      [key]: checked,
    }));
  }

  async function handleSave() {
    try {
      setSaving(true);
      setError(null);

      // Determine what changed
      const originalSelected = {};
      if (formData.userAuthorities) {
        formData.userAuthorities.forEach(auth => {
          const prefix = `${module}${roleSeparator}${entityType}${roleSeparator}`;
          if (auth.startsWith(prefix)) {
            const rest = auth.substring(prefix.length);
            const sepIdx = rest.indexOf(roleSeparator);
            if (sepIdx > 0) {
              const role = rest.substring(0, sepIdx);
              const entityId = rest.substring(sepIdx + 1);
              const key = `${entityId}:${role}`;
              originalSelected[key] = true;
            }
          }
        });
      }

      // Find additions and removals
      const toAdd = [];
      const toRemove = [];

      Object.keys(selectedPermissions).forEach(key => {
        if (selectedPermissions[key] && !originalSelected[key]) {
          const [entityId, role] = key.split(':');
          toAdd.push({ module, entityType, role, entityId });
        }
      });

      Object.keys(originalSelected).forEach(key => {
        if (!selectedPermissions[key]) {
          const [entityId, role] = key.split(':');
          toRemove.push({ module, entityType, role, entityId });
        }
      });

      // Apply changes
      for (const perm of toAdd) {
        await addPermission(username, perm);
      }

      for (const perm of toRemove) {
        await removePermission(username, perm.module, perm.entityType, perm.role, perm.entityId);
      }

      setToast({ type: 'success', message: 'Permissions saved successfully' });
      
      // Reload to get fresh data
      await loadData();
    } catch (err) {
      setError(err.message);
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  function renderEntitySection(entities, isPublic) {
    if (!entities || entities.length === 0) return null;

    return (
      <div className="mb-4">
        <h6 className={`d-flex align-items-center ${isPublic ? 'text-success' : 'text-warning'}`}>
          <i className={`bi ${isPublic ? 'bi-unlock' : 'bi-lock'} me-2`}></i>
          {isPublic ? 'Public' : 'Private'} {entityType}s
        </h6>
        
        <div className="table-responsive">
          <table className="table table-sm table-bordered">
            <thead className="table-light">
              <tr>
                <th style={{ minWidth: '200px' }}>{entityType}</th>
                {roles.map(role => (
                  <th key={role} className="text-center" style={{ minWidth: '100px' }}>
                    {role}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {entities.map(entity => {
                const canManage = canManageEntity(entity.id);
                return (
                  <tr key={entity.id} className={!canManage ? 'table-secondary' : ''}>
                    <td>
                      <div className="fw-medium">{entity.label}</div>
                      {entity.description && (
                        <small className="text-muted">{entity.description}</small>
                      )}
                    </td>
                    {roles.map(role => {
                      const key = `${entity.id}:${role}`;
                      const isChecked = !!selectedPermissions[key];
                      const isManagerRole = role === entityManagerRole;
                      
                      return (
                        <td key={role} className="text-center align-middle">
                          <input
                            type="checkbox"
                            className="form-check-input"
                            checked={isChecked}
                            onChange={(e) => handlePermissionChange(entity.id, role, e.target.checked)}
                            disabled={!canManage || (isManagerRole && !isAdmin && !supervisedModules.includes(module))}
                            title={!canManage ? 'You do not have permission to manage this entity' : ''}
                          />
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (error && !formData) {
    return (
      <div className="alert alert-danger">
        <i className="bi bi-exclamation-triangle me-2"></i>
        {error}
        <Link to={`/user/${encodeURIComponent(username)}`} className="btn btn-secondary ms-3">
          Back to User
        </Link>
      </div>
    );
  }

  return (
    <div className="role-manager-container">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2>
            <i className="bi bi-key me-2"></i>
            {entityType} Permissions
          </h2>
          <p className="text-muted mb-0">
            User: <strong>{username}</strong> | Database: <strong>{module}</strong>
          </p>
        </div>
        <Link to={`/user/${encodeURIComponent(username)}`} className="btn btn-outline-secondary">
          <i className="bi bi-arrow-left me-1"></i>
          Back to User
        </Link>
      </div>

      {/* Role Legend */}
      <div className="alert alert-info mb-4">
        <h6 className="alert-heading">
          <i className="bi bi-info-circle me-2"></i>
          Available Roles
        </h6>
        <div className="d-flex flex-wrap gap-2">
          {roles.map(role => (
            <span key={role} className="badge bg-secondary">
              {role}
              {role === entityManagerRole && (
                <i className="bi bi-star-fill ms-1" title="Manager role grants full access"></i>
              )}
            </span>
          ))}
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="alert alert-danger">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </div>
      )}

      {/* Permission Tables */}
      {formData && (
        <>
          {renderEntitySection(formData.publicEntities, true)}
          {formData.visibilitySupported && renderEntitySection(formData.privateEntities, false)}
          
          {(!formData.publicEntities || formData.publicEntities.length === 0) &&
           (!formData.privateEntities || formData.privateEntities.length === 0) && (
            <div className="alert alert-warning">
              <i className="bi bi-exclamation-triangle me-2"></i>
              No {entityType} entities found in this database.
            </div>
          )}
        </>
      )}

      {/* Save Button */}
      <div className="d-flex justify-content-end gap-2 mt-4">
        <Link to={`/user/${encodeURIComponent(username)}`} className="btn btn-secondary">
          Cancel
        </Link>
        <button 
          className="btn btn-primary" 
          onClick={handleSave}
          disabled={saving}
        >
          {saving ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              Saving...
            </>
          ) : (
            <>
              <i className="bi bi-check-lg me-1"></i>
              Save Permissions
            </>
          )}
        </button>
      </div>

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
