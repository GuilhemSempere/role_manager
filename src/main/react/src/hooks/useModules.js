import { useState, useEffect, useCallback } from 'react';
import { getModules } from '../api/roleManagerApi';

export function useModules() {
  const [modules, setModules] = useState({ publicModules: [], privateModules: [] });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadModules = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getModules();
      setModules(data);
    } catch (err) {
      setError(err.message);
      setModules({ publicModules: [], privateModules: [] });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadModules();
  }, [loadModules]);

  function refresh() {
    return loadModules();
  }

  // Combine all modules for convenience
  const allModules = [...modules.publicModules, ...modules.privateModules];

  return {
    publicModules: modules.publicModules,
    privateModules: modules.privateModules,
    allModules,
    isAdmin: modules.isAdmin,
    loading,
    error,
    refresh,
  };
}
