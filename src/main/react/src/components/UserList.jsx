import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useUsers } from '../hooks/useUsers';
import { useAuth } from '../context/AuthContext';
import { deleteUser } from '../api/roleManagerApi';
import ConfirmModal from './common/ConfirmModal';
import Toast from './common/Toast';

export default function UserList() {
  const { canWriteToSystem, isAdmin } = useAuth();
  const {
    users,
    totalCount,
    currentPage,
    totalPages,
    pageSize,
    searchTerm,
    loading,
    error,
    search,
    goToPage,
    refresh,
  } = useUsers('', 20);

  const [searchInput, setSearchInput] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState(null);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      search(searchInput);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput, search]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    
    try {
      setDeleting(true);
      await deleteUser(deleteTarget);
      setToast({ type: 'success', message: `User "${deleteTarget}" deleted successfully` });
      refresh();
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    return (
      <nav aria-label="User list pagination">
        <ul className="pagination pagination-sm mb-0">
          <li className={`page-item ${currentPage === 0 ? 'disabled' : ''}`}>
            <button 
              className="page-link" 
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 0}
            >
              <i className="bi bi-chevron-left"></i>
            </button>
          </li>
          
          {start > 0 && (
            <>
              <li className="page-item">
                <button className="page-link" onClick={() => goToPage(0)}>1</button>
              </li>
              {start > 1 && (
                <li className="page-item disabled">
                  <span className="page-link">...</span>
                </li>
              )}
            </>
          )}
          
          {pages.map(page => (
            <li key={page} className={`page-item ${page === currentPage ? 'active' : ''}`}>
              <button className="page-link" onClick={() => goToPage(page)}>
                {page + 1}
              </button>
            </li>
          ))}
          
          {end < totalPages && (
            <>
              {end < totalPages - 1 && (
                <li className="page-item disabled">
                  <span className="page-link">...</span>
                </li>
              )}
              <li className="page-item">
                <button className="page-link" onClick={() => goToPage(totalPages - 1)}>
                  {totalPages}
                </button>
              </li>
            </>
          )}
          
          <li className={`page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}`}>
            <button 
              className="page-link" 
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
            >
              <i className="bi bi-chevron-right"></i>
            </button>
          </li>
        </ul>
      </nav>
    );
  };

  return (
    <div className="role-manager-container">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-people me-2"></i>
          User Management
        </h2>
        {canWriteToSystem && (
          <Link to="/user/new" className="btn btn-primary">
            <i className="bi bi-person-plus me-1"></i>
            New User
          </Link>
        )}
      </div>

      {/* Search Box */}
      <div className="card mb-4">
        <div className="card-body">
          <div className="row align-items-center">
            <div className="col-md-6">
              <div className="search-box">
                <i className="bi bi-search"></i>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search by username..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                />
              </div>
            </div>
            <div className="col-md-6 text-md-end mt-3 mt-md-0">
              <span className="pagination-info">
                {totalCount} user{totalCount !== 1 ? 's' : ''} found
                {searchTerm && ` matching "${searchTerm}"`}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="alert alert-danger" role="alert">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </div>
      )}

      {/* User Table */}
      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover user-table mb-0">
            <thead>
              <tr>
                <th>Username</th>
                <th>Permissions Summary</th>
                <th>Auth Method</th>
                <th style={{ width: '150px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4" className="text-center py-4">
                    <div className="spinner-border spinner-border-sm text-primary me-2" role="status">
                      <span className="visually-hidden">Loading...</span>
                    </div>
                    Loading users...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan="4" className="text-center text-muted py-4">
                    <i className="bi bi-inbox fs-1 d-block mb-2"></i>
                    No users found
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.username}>
                    <td>
                      <Link to={`/user/${encodeURIComponent(user.username)}`} className="fw-medium">
                        {user.username}
                      </Link>
                    </td>
                    <td>
                      <span className="text-muted" style={{ fontSize: '0.9em' }}>
                        {user.authoritySummary || '-'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${user.method === 'Local' ? 'bg-secondary' : 'bg-info'}`}>
                        {user.method}
                      </span>
                    </td>
                    <td>
                      <div className="btn-group btn-group-sm">
                        <Link 
                          to={`/user/${encodeURIComponent(user.username)}`} 
                          className="btn btn-outline-primary action-btn"
                          title="Edit"
                        >
                          <i className="bi bi-pencil"></i>
                        </Link>
                        {canWriteToSystem && (
                          <>
                            <Link 
                              to={`/user/${encodeURIComponent(user.username)}/clone`} 
                              className="btn btn-outline-secondary action-btn"
                              title="Clone"
                            >
                              <i className="bi bi-copy"></i>
                            </Link>
                            <button 
                              className="btn btn-outline-danger action-btn"
                              title="Delete"
                              onClick={() => setDeleteTarget(user.username)}
                            >
                              <i className="bi bi-trash"></i>
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="card-footer d-flex justify-content-between align-items-center">
            <small className="text-muted">
              Showing {currentPage * pageSize + 1} - {Math.min((currentPage + 1) * pageSize, totalCount)} of {totalCount}
            </small>
            {renderPagination()}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        show={!!deleteTarget}
        title="Delete User"
        message={`Are you sure you want to delete user "${deleteTarget}"? This action cannot be undone.`}
        confirmLabel="Delete"
        confirmVariant="danger"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* Toast Notifications */}
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
