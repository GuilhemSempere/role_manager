import { useState, useEffect, useCallback } from 'react';
import { getPermissionFormData, addPermission, removePermission } from '../api/roleManagerApi';

export function usePermissions(username, module, entityType) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async () => {
    if (!username || !module || !entityType) return;
    
    try {
      setLoading(true);
      setError(null);
      const permData = await getPermissionFormData(username, module, entityType);
      setData(permData);
    } catch (err) {
      setError(err.message);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [username, module, entityType]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  async function add(permission) {
    try {
      setSaving(true);
      setError(null);
      await addPermission(username, permission);
      await loadData(); // Refresh
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function remove(module, entityType, role, entityId) {
    try {
      setSaving(true);
      setError(null);
      await removePermission(username, module, entityType, role, entityId);
      await loadData(); // Refresh
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  function refresh() {
    return loadData();
  }

  return {
    data,
    loading,
    error,
    saving,
    add,
    remove,
    refresh,
  };
}
