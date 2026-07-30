import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getProcessList, getDumpStatusPageUrl } from '../api/roleManagerApi';
import Toast from './common/Toast';

const POLL_INTERVAL_MS = 2000;

export default function AdminProcesses() {
  const { isAdmin, supervisedModules, loading: authLoading } = useAuth();
  const [processes, setProcesses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const intervalRef = useRef(null);

  const canViewProcesses = isAdmin || supervisedModules.length > 0;

  const loadProcesses = useCallback(async () => {
    try {
      const result = await getProcessList();
      setProcesses(Array.isArray(result) ? result : []);
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading || !canViewProcesses) {
      return undefined;
    }

    loadProcesses();
    intervalRef.current = setInterval(loadProcesses, POLL_INTERVAL_MS);
    return () => clearInterval(intervalRef.current);
  }, [authLoading, canViewProcesses, loadProcesses]);

  if (authLoading) {
    return null;
  }

  if (!canViewProcesses) {
    return (
      <div className="alert alert-danger" role="alert">
        You are not allowed to access the process list page.
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="mb-0 text-dark fw-semibold">Admin processes</h2>
        <button className="btn btn-sm btn-primary" onClick={loadProcesses} disabled={loading}>
          <i className="bi bi-arrow-clockwise me-1"></i>
          Refresh
        </button>
      </div>

      <div className="table-responsive">
        <table className="table table-sm table-striped align-middle">
          <thead>
            <tr>
              <th>Process ID</th>
              <th>Type</th>
              <th>Database</th>
              <th>Status</th>
              <th>Execution message</th>
              <th className="text-center">Details</th>
            </tr>
          </thead>
          <tbody>
            {processes.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-muted">
                  {loading ? 'Loading...' : 'No active or recent processes'}
                </td>
              </tr>
            ) : (
              processes.map((process) => (
                <tr key={process.processID}>
                  <td>{process.processID}</td>
                  <td>{process.type}</td>
                  <td>{process.module}</td>
                  <td>{process.status}</td>
                  <td>{process.message}</td>
                  <td className="text-center">
                    {process.type !== 'IMPORT' && (
                      <a
                        href={getDumpStatusPageUrl(process.module, process.processID)}
                        target="_blank"
                        rel="noreferrer"
                        title="View details"
                      >
                        <i className="bi bi-search"></i>
                      </a>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {toast && (
        <Toast type={toast.type} message={toast.message} onClose={() => setToast(null)} />
      )}
    </div>
  );
}
