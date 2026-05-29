/**
 * API service layer for Role Manager React frontend.
 * All API calls to the Spring backend go through this module.
 */

function getContextPath() {
  const extract = (pathname) => {
    for (const marker of ['/private/roleManager', '/roleManager']) {
      const index = pathname.indexOf(marker);
      if (index === 0) {
        return '';
      }
      if (index > 0) {
        return pathname.slice(0, index);
      }
    }
    return null;
  };

  const fromLocation = extract(window.location.pathname);
  if (fromLocation !== null) {
    return fromLocation;
  }

  const basePathname = new URL(document.baseURI).pathname;
  const fromBase = extract(basePathname);
  if (fromBase !== null) {
    return fromBase;
  }

  // Fallback for URLs like /Gigwa2/ where marker segments are not present.
  const segments = window.location.pathname.split('/').filter(Boolean);
  if (segments.length === 1 && segments[0] !== 'private' && segments[0] !== 'roleManager') {
    return `/${segments[0]}`;
  }

  return '';
}

const CONTEXT_PATH = getContextPath();
const API_BASE = `${CONTEXT_PATH}/private/roleManager/api`;
const LEGACY_BASE = `${CONTEXT_PATH}/private/roleManager`;
const BACKOFFICE_BASE = `${CONTEXT_PATH}/private`;

export function getAppContextPath() {
  return CONTEXT_PATH;
}

function withJsonSuffix(endpoint) {
  const [path, query] = endpoint.split('?');
  const suffixedPath = path.endsWith('.json_') ? path : `${path}.json_`;
  return query ? `${suffixedPath}?${query}` : suffixedPath;
}

/**
 * Generic fetch wrapper with error handling
 */
async function apiFetch(endpoint, options = {}) {
  const url = `${API_BASE}${withJsonSuffix(endpoint)}`;
  
  const defaultOptions = {
    credentials: 'same-origin', // Include cookies for session auth
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  const response = await fetch(url, { ...defaultOptions, ...options });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      // Session expired or unauthorized - redirect to login
      window.location.href = `${CONTEXT_PATH}/login`;
      throw new Error('Unauthorized');
    }
    
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || errorData.message || `HTTP ${response.status}`);
  }

  return response.json();
}

// ============================================================
// Role Configuration API
// ============================================================

/**
 * Get the role configuration from roles.properties
 * Returns: level1Types, level1Roles, level2Types, level2Roles, roleSeparator, etc.
 */
export async function getRoleConfig() {
  return apiFetch('/roleConfig');
}

// ============================================================
// Current User API
// ============================================================

/**
 * Get current logged-in user info
 */
export async function getCurrentUser() {
  return apiFetch('/currentUser');
}

// ============================================================
// User Management API
// ============================================================

/**
 * List users with pagination
 * Uses the existing listUsers.json_ endpoint
 */
export async function listUsers(loginLookup = '', page = 0, size = 20) {
  const params = new URLSearchParams({
    loginLookup,
    page: page.toString(),
    size: size.toString(),
  });
  
  // Use the existing endpoint (not in /api)
  const response = await fetch(`${LEGACY_BASE}/listUsers.json_?${params}`, {
    credentials: 'same-origin',
  });
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  
  return response.json();
}

/**
 * Count users matching login lookup
 */
export async function countUsers(loginLookup = '') {
  const response = await fetch(`${LEGACY_BASE}/countUsers.json_?loginLookup=${encodeURIComponent(loginLookup)}`, {
    credentials: 'same-origin',
  });
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  
  return response.json();
}

/**
 * Get user details by username
 */
export async function getUser(username) {
  return apiFetch(`/user/${encodeURIComponent(username)}`);
}

/**
 * Create a new user
 */
export async function createUser(userData) {
  return apiFetch('/user', {
    method: 'POST',
    body: JSON.stringify(userData),
  });
}

/**
 * Update an existing user
 */
export async function updateUser(username, userData) {
  return apiFetch(`/user/${encodeURIComponent(username)}`, {
    method: 'PUT',
    body: JSON.stringify(userData),
  });
}

/**
 * Delete a user
 * Uses the existing removeUser.json_ endpoint
 */
export async function deleteUser(username) {
  const response = await fetch(`${LEGACY_BASE}/removeUser.json_?user=${encodeURIComponent(username)}`, {
    method: 'DELETE',
    credentials: 'same-origin',
  });
  
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `HTTP ${response.status}`);
  }
  
  return response.json();
}

// ============================================================
// Modules API
// ============================================================

/**
 * Get list of modules accessible to current user
 */
export async function getModules() {
  return apiFetch('/modules');
}

/**
 * Get entities for a specific module and entity type
 */
export async function getModuleEntities(module, entityType, parentEntityId = null) {
  const params = new URLSearchParams({ entityType });
  if (parentEntityId) {
    params.append('parentEntityId', parentEntityId);
  }
  return apiFetch(`/module/${encodeURIComponent(module)}/entities?${params}`);
}

export async function getModuleSubEntities(module, entityType, mainEntityId) {
  const params = new URLSearchParams({
    entityType,
    mainEntityId: String(mainEntityId),
  });
  return apiFetch(`/module/${encodeURIComponent(module)}/subEntities?${params}`);
}

// ============================================================
// Permission Management API
// ============================================================

/**
 * Get permission form data for a user/module/entityType combination
 */
export async function getPermissionFormData(username, module, entityType) {
  const params = new URLSearchParams({ username, module, entityType });
  return apiFetch(`/permissionForm?${params}`);
}

/**
 * Add a permission to a user
 */
export async function addPermission(username, permission) {
  return apiFetch(`/user/${encodeURIComponent(username)}/permission`, {
    method: 'POST',
    body: JSON.stringify(permission),
  });
}

/**
 * Remove a permission from a user
 */
export async function removePermission(username, module, entityType, role, entityId) {
  const params = new URLSearchParams({ module, entityType, role, entityId });
  return apiFetch(`/user/${encodeURIComponent(username)}/permission?${params}`, {
    method: 'DELETE',
  });
}

// ============================================================
// Database Management API (legacy back-office endpoints)
// ============================================================

export async function listDatabases() {
  const response = await fetch(`${BACKOFFICE_BASE}/listModules.json_`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function listDatabaseHosts() {
  const response = await fetch(`${BACKOFFICE_BASE}/hosts.json_`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function createDatabase(moduleName, host) {
  const params = new URLSearchParams({ module: moduleName, host });
  const response = await fetch(`${BACKOFFICE_BASE}/createModule.json_?${params}`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function deleteDatabase(moduleName, removeDumps = true) {
  const params = new URLSearchParams({
    module: moduleName,
    removeDumps: String(removeDumps),
  });
  const response = await fetch(`${BACKOFFICE_BASE}/removeModule.json_?${params}`, {
    method: 'DELETE',
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function updateDatabaseDetails(moduleName, isPublic, isHidden, category) {
  const params = new URLSearchParams({
    module: moduleName,
    public: String(Boolean(isPublic)),
    hidden: String(Boolean(isHidden)),
    category: category || '',
  });
  const response = await fetch(`${BACKOFFICE_BASE}/moduleDetails.json_?${params}`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function getDatabaseDumpInfo(moduleName) {
  const params = new URLSearchParams({ module: moduleName });
  const response = await fetch(`${BACKOFFICE_BASE}/moduleDumpInfo.json_?${params}`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function getModuleEntityInfo(moduleName, entityType, allLevelEntityIDs) {
  const response = await fetch(`${BACKOFFICE_BASE}/moduleEntityInfo.json_`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      module: moduleName,
      entityType,
      allLevelEntityIDs,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => '');
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  return response.text();
}

export async function removeModuleEntity(moduleName, entityType, allLevelEntityIDs) {
  const response = await fetch(`${BACKOFFICE_BASE}/removeModuleEntity.json_`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      module: moduleName,
      entityType,
      allLevelEntityIDs,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => '');
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  const payload = await response.text();
  return payload === 'true' || payload === '"true"' || payload === '1';
}

export async function updateModuleEntityVisibility(moduleName, entityType, entityId, isPublic) {
  const params = new URLSearchParams({
    module: moduleName,
    entityType,
    entityId: String(entityId),
    public: String(Boolean(isPublic)),
  });
  const response = await fetch(`${BACKOFFICE_BASE}/entityVisibility.json_?${params}`, {
    method: 'POST',
    credentials: 'same-origin',
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => '');
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  const payload = await response.text();
  return payload === 'true' || payload === '"true"' || payload === '1';
}

export async function updateModuleEntityDescription(moduleName, entityType, entityId, description) {
  const params = new URLSearchParams({
    module: moduleName,
    entityType,
    entityId: String(entityId),
    desc: description || '',
  });
  const response = await fetch(`${BACKOFFICE_BASE}/entityDescUpdate.json_?${params}`, {
    method: 'POST',
    credentials: 'same-origin',
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => '');
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  const payload = await response.text();
  return payload === 'true' || payload === '"true"' || payload === '1';
}

export async function getDumpRestoreWarning(moduleName, dumpId) {
  const params = new URLSearchParams({ module: moduleName, dump: dumpId });
  const response = await fetch(`${BACKOFFICE_BASE}/dumpRestoreWarning.do_?${params}`, {
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.text();
}

export async function deleteDatabaseDump(moduleName, dumpId) {
  const params = new URLSearchParams({ module: moduleName, dump: dumpId });
  const response = await fetch(`${BACKOFFICE_BASE}/deleteDump.json_?${params}`, {
    method: 'DELETE',
    credentials: 'same-origin',
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export function getDumpDownloadUrl(moduleName, dumpId) {
  const params = new URLSearchParams({ module: moduleName, dumpId });
  return `${BACKOFFICE_BASE}/moduleDumpDownload.json_?${params}`;
}

export function getDumpLogDownloadUrl(moduleName, dumpId) {
  const params = new URLSearchParams({ module: moduleName, dumpId });
  return `${BACKOFFICE_BASE}/moduleDumpLogDownload.json_?${params}`;
}

export function getNewDumpUrl(moduleName, dumpName, description) {
  const params = new URLSearchParams({
    module: moduleName,
    name: dumpName,
    description: description || '',
  });
  return `${BACKOFFICE_BASE}/newDump.do_?${params}`;
}

export function getRestoreDumpUrl(moduleName, dumpId, dropBeforeRestore) {
  const params = new URLSearchParams({
    module: moduleName,
    dump: dumpId,
    drop: String(Boolean(dropBeforeRestore)),
  });
  return `${BACKOFFICE_BASE}/restoreDump.do_?${params}`;
}

// ============================================================
// Utility Functions
// ============================================================

/**
 * Parse an authority string into its components
 * Format: MODULE$ENTITY_TYPE$ROLE$ENTITY_ID
 */
export function parseAuthority(authority, separator = '$') {
  const parts = authority.split(separator);
  if (parts.length === 4) {
    return {
      module: parts[0],
      entityType: parts[1],
      role: parts[2],
      entityId: parts[3],
    };
  } else if (parts.length === 2) {
    // Module-level role like MODULE$SUPERVISOR
    return {
      module: parts[0],
      role: parts[1],
    };
  }
  return { raw: authority };
}

/**
 * Build an authority string from components
 */
export function buildAuthority(module, entityType, role, entityId, separator = '$') {
  if (entityType && entityId) {
    return `${module}${separator}${entityType}${separator}${role}${separator}${entityId}`;
  } else {
    return `${module}${separator}${role}`;
  }
}
