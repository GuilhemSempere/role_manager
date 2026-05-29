import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRoleConfig } from '../context/RoleConfigContext';

export default function Layout({ children }) {
  const { currentUser, loading: authLoading } = useAuth();
  const { roleDbCreator, loading: configLoading } = useRoleConfig();
  const location = useLocation();

  const isLoading = authLoading || configLoading;
  const hasDbCreatorRole = Boolean(
    currentUser?.authorities?.includes(roleDbCreator)
  );
  const canManageDatabases = Boolean(currentUser?.isAdmin || hasDbCreatorRole);

  return (
    <div className="min-vh-100 bg-light">
      {/* Navbar */}
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
        <div className="container">
          <Link className="navbar-brand" to="/">
            <i className="bi bi-shield-lock me-2"></i>
            Role Manager
          </Link>
          
          <button 
            className="navbar-toggler" 
            type="button" 
            data-bs-toggle="collapse" 
            data-bs-target="#navbarNav"
          >
            <span className="navbar-toggler-icon"></span>
          </button>
          
          <div className="collapse navbar-collapse" id="navbarNav">
            <ul className="navbar-nav me-auto">
              <li className="nav-item">
                <Link 
                  className={`nav-link ${location.pathname === '/' || location.pathname === '/users' ? 'active' : ''}`} 
                  to="/users"
                >
                  <i className="bi bi-people me-1"></i>
                  Users
                </Link>
              </li>
              {canManageDatabases && (
                <li className="nav-item">
                  <Link
                    className={`nav-link ${location.pathname === '/databases' ? 'active' : ''}`}
                    to="/databases"
                  >
                    <i className="bi bi-hdd-stack me-1"></i>
                    Manage databases
                  </Link>
                </li>
              )}
            </ul>
            
            {currentUser?.authenticated && (
              <span className="navbar-text">
                <i className="bi bi-person-circle me-1"></i>
                {currentUser.username}
                {currentUser.isAdmin && (
                  <span className="badge bg-warning text-dark ms-2">Admin</span>
                )}
              </span>
            )}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="container py-4">
        {isLoading ? (
          <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : (
          children
        )}
      </main>

      {/* Footer */}
      <footer className="bg-light border-top py-3 mt-auto">
        <div className="container text-center text-muted">
          <small>Role Manager - User Permission Management</small>
        </div>
      </footer>
    </div>
  );
}
