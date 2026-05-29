import { createContext, useContext, useState, useEffect } from 'react';
import { getRoleConfig } from '../api/roleManagerApi';

const RoleConfigContext = createContext(null);

export function RoleConfigProvider({ children }) {
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadConfig();
  }, []);

  async function loadConfig() {
    try {
      setLoading(true);
      const roleConfig = await getRoleConfig();
      setConfig(roleConfig);
      setError(null);
    } catch (err) {
      setError(err.message);
      setConfig(null);
    } finally {
      setLoading(false);
    }
  }

  function refresh() {
    return loadConfig();
  }

  /**
   * Get roles for a level 1 entity type (e.g., "project")
   */
  function getLevel1Roles(entityType) {
    if (!config || !config.level1Roles) return [];
    return config.level1Roles[entityType] || [];
  }

  /**
   * Get roles for a level 2 entity type (e.g., "project.run")
   */
  function getLevel2Roles(parentType, subType) {
    if (!config || !config.level2Roles) return [];
    const parentRoles = config.level2Roles[parentType];
    if (!parentRoles) return [];
    return parentRoles[subType] || [];
  }

  /**
   * Check if an entity type is level 1
   */
  function isLevel1Type(entityType) {
    if (!config || !config.level1Types) return false;
    return config.level1Types.includes(entityType);
  }

  /**
   * Check if an entity type is level 2
   */
  function isLevel2Type(entityType) {
    if (!config || !config.level2Types) return false;
    return config.level2Types.includes(entityType);
  }

  /**
   * Parse a level 2 type into parent and sub type
   */
  function parseLevel2Type(level2Type) {
    const parts = level2Type.split('.');
    if (parts.length === 2) {
      return { parent: parts[0], sub: parts[1] };
    }
    return null;
  }

  const value = {
    config,
    loading,
    error,
    refresh,
    // Convenience accessors
    level1Types: config?.level1Types || [],
    level2Types: config?.level2Types || [],
    roleSeparator: config?.roleSeparator || '$',
    entityManagerRole: config?.entityManagerRole || 'MANAGER',
    roleAdmin: config?.roleAdmin || 'ROLE_ADMIN',
    roleDbSupervisor: config?.roleDbSupervisor || 'ROLE_DB_SUPERVISOR',
    roleDbCreator: config?.roleDbCreator || 'ROLE_DB_CREATOR',
    // Helper functions
    getLevel1Roles,
    getLevel2Roles,
    isLevel1Type,
    isLevel2Type,
    parseLevel2Type,
  };

  return (
    <RoleConfigContext.Provider value={value}>
      {children}
    </RoleConfigContext.Provider>
  );
}

export function useRoleConfig() {
  const context = useContext(RoleConfigContext);
  if (!context) {
    throw new Error('useRoleConfig must be used within a RoleConfigProvider');
  }
  return context;
}
