import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useRoleConfig } from '../context/RoleConfigContext';
import {
  createDatabase,
  deleteDatabase,
  deleteDatabaseDump,
  getAppContextPath,
  getModuleEntityInfo,
  getModuleEntities,
  getModuleSubEntities,
  getDatabaseDumpInfo,
  getDumpDownloadUrl,
  getDumpLogDownloadUrl,
  getDumpRestoreWarning,
  getNewDumpUrl,
  removeModuleEntity,
  getRestoreDumpUrl,
  listDatabaseHosts,
  listDatabases,
  updateModuleEntityDescription,
  updateModuleEntityVisibility,
  updateDatabaseDetails,
} from '../api/roleManagerApi';
import Toast from './common/Toast';

async function tryLoadCustomizationScript(src) {
  return new Promise((resolve) => {
    const existing = document.querySelector(`script[data-db-customization-src="${src}"]`);
    if (existing) {
      resolve(true);
      return;
    }

    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.dataset.dbCustomizationSrc = src;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.head.appendChild(script);
  });
}

async function loadDatabaseListCustomization() {
  if (window.roleManagerDatabaseListCustomization) {
    return window.roleManagerDatabaseListCustomization;
  }

  const configuredUrl = window.roleManagerDatabaseListCustomizationUrl;
  const defaultUrl = `${getAppContextPath()}/js/moduleListCustomisation.react.js`;
  const candidates = [configuredUrl, defaultUrl].filter(Boolean);

  for (const src of candidates) {
    const loaded = await tryLoadCustomizationScript(src);
    if (loaded && window.roleManagerDatabaseListCustomization) {
      return window.roleManagerDatabaseListCustomization;
    }
  }

  return null;
}

function parseCategoryForEdition(category) {
  const raw = category || '';
  return {
    categoryLabel: raw,
    categoryReadOnly: false,
    categoryTitle: '',
  };
}

function buildCategoryForSave(edit) {
  return edit?.categoryLabel || '';
}

function formatSize(sizeValue) {
  if (typeof sizeValue !== 'number' || Number.isNaN(sizeValue)) {
    return '-';
  }
  if (sizeValue >= 1024 * 1024 * 1024) {
    return `${Math.round((sizeValue / (1024 * 1024 * 1024)) * 10) / 10} GB`;
  }
  if (sizeValue >= 1024 * 1024) {
    return `${Math.round((sizeValue / (1024 * 1024)) * 10) / 10} MB`;
  }
  if (sizeValue >= 1024) {
    return `${Math.round((sizeValue / 1024) * 10) / 10} KB`;
  }
  return `${Math.round(sizeValue * 10) / 10} B`;
}

const dumpValidityTips = {
  VALID: 'Up to date: A dump is available for the current database contents',
  OUTDATED: 'Out of date: Existing dumps were created before the last change to this database',
  DIVERGED: "Diverged: Database was restored using a dump that wasn't the most recent one",
  BUSY: 'Busy: Database is locked (either data is being imported or a dump is being created / restored)',
  NONE: 'Unavailable: No dump exists for this database',
  UNSUPPORTED: 'Unsupported: No dumps may be managed for this database',
};

function getDumpValidityClass(validity) {
  const normalized = String(validity || '').toUpperCase();
  return normalized ? `dump${normalized}` : '';
}

function canEditModule(currentUser, isAdmin, moduleName) {
  if (isAdmin) {
    return true;
  }
  return Boolean(currentUser?.supervisedModules?.includes(moduleName));
}

export default function DatabaseManager() {
  const { currentUser, isAdmin } = useAuth();
  const { roleDbCreator, level1Types, level2Types } = useRoleConfig();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);
  const [databases, setDatabases] = useState({});
  const [hosts, setHosts] = useState([]);
  const [moduleName, setModuleName] = useState('');
  const [host, setHost] = useState('');
  const [customization, setCustomization] = useState(null);
  const [edits, setEdits] = useState({});
  const [dumpDialog, setDumpDialog] = useState({
    open: false,
    moduleName: null,
    loading: false,
    error: null,
    dumps: [],
    locked: false,
  });
  const [newDumpName, setNewDumpName] = useState('');
  const [newDumpDescription, setNewDumpDescription] = useState('');
  const [restoreCandidate, setRestoreCandidate] = useState(null);
  const [restoreWarning, setRestoreWarning] = useState('');
  const [dropBeforeRestore, setDropBeforeRestore] = useState(true);
  const [contentDialog, setContentDialog] = useState({
    open: false,
    title: '',
    url: '',
  });
  const [entityDialog, setEntityDialog] = useState({
    open: false,
    moduleName: '',
    entityType: '',
    loading: false,
    error: null,
    visibilitySupported: false,
    descriptionSupported: false,
    roles: [],
    subEntityTypes: [],
    subEntitiesByEntityId: {},
    publicEntities: [],
    privateEntities: [],
    drafts: {},
    originalDrafts: {},
    savingDescriptionKey: null,
  });

  const hasDbCreatorRole = Boolean(currentUser?.authorities?.includes(roleDbCreator));
  const canCreateDatabases = Boolean(isAdmin || hasDbCreatorRole);

  const databaseEntries = useMemo(() => {
    return Object.entries(databases).sort(([nameA], [nameB]) => nameA.localeCompare(nameB));
  }, [databases]);

  const dumpManagementEnabled = useMemo(() => {
    return Object.values(databases).some((details) => Object.prototype.hasOwnProperty.call(details || {}, 'dumpStatus'));
  }, [databases]);

  const columnLabels = useMemo(() => {
    const defaults = [
      { id: 'database', label: 'Database' },
      { id: 'host', label: 'Host' },
      { id: 'category', label: 'Category / Taxon' },
      { id: 'size', label: 'Size' },
      { id: 'entityManagement', label: 'Entity management' },
      { id: 'public', label: 'Public' },
      { id: 'hidden', label: 'Hidden' },
      { id: 'dumpStatus', label: 'Dumps' },
    ];

    if (!customization?.getColumns) {
      return defaults.reduce((accumulator, column) => ({ ...accumulator, [column.id]: column.label }), {});
    }

    try {
      const customized = customization.getColumns(defaults.map((column) => ({ ...column })));
      if (!Array.isArray(customized) || customized.length === 0) {
        return defaults.reduce((accumulator, column) => ({ ...accumulator, [column.id]: column.label }), {});
      }

      return customized.reduce((accumulator, column) => {
        accumulator[column.id] = column.label || column.id;
        return accumulator;
      }, {});
    } catch (err) {
      console.warn('Unable to apply database column customization:', err);
      return defaults.reduce((accumulator, column) => ({ ...accumulator, [column.id]: column.label }), {});
    }
  }, [customization]);

  const pageLinks = useMemo(() => {
    if (!customization?.getPageLinks) {
      return [];
    }

    try {
      const links = customization.getPageLinks({ isAdmin, canCreateDatabases });
      return Array.isArray(links) ? links : [];
    } catch (err) {
      console.warn('Unable to apply page link customization:', err);
      return [];
    }
  }, [customization, isAdmin, canCreateDatabases]);

  function parseCategoryWithCustomization(module, details) {
    const base = parseCategoryForEdition(details?.category);
    if (!customization?.parseCategoryForEdition) {
      return base;
    }

    try {
      const custom = customization.parseCategoryForEdition({
        moduleName: module,
        details,
        category: details?.category || '',
        defaults: { ...base },
      });
      if (custom && typeof custom === 'object') {
        return {
          ...base,
          ...custom,
          categoryLabel: custom.categoryLabel ?? base.categoryLabel,
        };
      }
    } catch (err) {
      console.warn('Unable to apply category parse customization:', err);
    }

    return base;
  }

  function buildCategoryValue(module, details, edit) {
    if (!customization?.buildCategoryForSave) {
      return buildCategoryForSave(edit);
    }

    try {
      return customization.buildCategoryForSave({
        moduleName: module,
        details,
        edit,
        defaultCategoryValue: buildCategoryForSave(edit),
      });
    } catch (err) {
      console.warn('Unable to apply category save customization:', err);
      return buildCategoryForSave(edit);
    }
  }

  async function loadData() {
    setLoading(true);
    setError(null);

    try {
      const customizationPromise = Promise.race([
        loadDatabaseListCustomization(),
        new Promise((resolve) => setTimeout(() => resolve(null), 2000)),
      ]);

      const [customizationResult, hostsResult, databasesResult] = await Promise.allSettled([
        customizationPromise,
        listDatabaseHosts(),
        listDatabases(),
      ]);

      const activeCustomization = customizationResult.status === 'fulfilled' ? customizationResult.value : null;
      setCustomization(activeCustomization);

      if (hostsResult.status === 'fulfilled') {
        const hostPayload = hostsResult.value;
        const hostList = Array.isArray(hostPayload)
          ? hostPayload
          : Array.isArray(hostPayload?.hosts)
            ? hostPayload.hosts
            : Array.isArray(hostPayload?.items)
              ? hostPayload.items
              : Object.values(hostPayload || {});
        const filteredHosts = hostList.filter((entry) => typeof entry === 'string' && entry.trim());
        setHosts(filteredHosts);
        setHost((previousHost) => {
          if (previousHost && filteredHosts.includes(previousHost)) {
            return previousHost;
          }
          return filteredHosts[0] || '';
        });
      } else {
        setHosts([]);
        setHost('');
      }

      if (databasesResult.status === 'fulfilled') {
        const databasePayload = databasesResult.value;
        const normalizedDatabases = databasePayload?.modules || databasePayload?.data || databasePayload || {};
        setDatabases(normalizedDatabases && typeof normalizedDatabases === 'object' ? normalizedDatabases : {});

        const parsedEdits = Object.entries(normalizedDatabases && typeof normalizedDatabases === 'object' ? normalizedDatabases : {}).reduce((accumulator, [module, details]) => {
          const parsed = activeCustomization?.parseCategoryForEdition
            ? (() => {
                try {
                  const base = parseCategoryForEdition(details?.category);
                  const custom = activeCustomization.parseCategoryForEdition({
                    moduleName: module,
                    details,
                    category: details?.category || '',
                    defaults: { ...base },
                  });
                  if (custom && typeof custom === 'object') {
                    return {
                      ...base,
                      ...custom,
                      categoryLabel: custom.categoryLabel ?? base.categoryLabel,
                    };
                  }
                  return base;
                } catch (err) {
                  console.warn('Unable to apply category parse customization:', err);
                  return parseCategoryForEdition(details?.category);
                }
              })()
            : parseCategoryForEdition(details?.category);
          accumulator[module] = {
            categoryLabel: parsed.categoryLabel,
            categoryReadOnly: Boolean(parsed.categoryReadOnly),
            categoryTitle: parsed.categoryTitle || '',
            public: Boolean(details?.public),
            hidden: Boolean(details?.hidden),
            dirty: false,
          };
          return accumulator;
        }, {});
        setEdits(parsedEdits);
      } else {
        throw databasesResult.reason instanceof Error ? databasesResult.reason : new Error('Unable to load databases.');
      }
    } catch (err) {
      setError(err.message || 'Unable to load database list.');
      setHosts([]);
      setDatabases({});
      setEdits({});
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  function isSameOriginUrl(rawUrl) {
    try {
      const resolved = new URL(rawUrl, window.location.origin);
      return resolved.origin === window.location.origin;
    } catch (err) {
      return false;
    }
  }

  function openContentDialog(url, title) {
    setContentDialog({
      open: true,
      title,
      url,
    });
  }

  function closeContentDialog() {
    setContentDialog({
      open: false,
      title: '',
      url: '',
    });
  }

  async function openEntityDialog(module, entityType) {
    setEntityDialog({
      open: true,
      moduleName: module,
      entityType,
      loading: true,
      error: null,
      visibilitySupported: false,
      descriptionSupported: false,
      roles: [],
      subEntityTypes: [],
      subEntitiesByEntityId: {},
      publicEntities: [],
      privateEntities: [],
      drafts: {},
      originalDrafts: {},
      savingDescriptionKey: null,
    });

    try {
      const result = await getModuleEntities(module, entityType);
      const publicEntities = Array.isArray(result?.publicEntities) ? result.publicEntities : [];
      const privateEntities = Array.isArray(result?.privateEntities) ? result.privateEntities : [];
      const apiSubEntityTypes = Array.isArray(result?.subEntityTypes) ? result.subEntityTypes : [];
      const configuredSubEntityTypes = Array.isArray(level2Types)
        ? level2Types
            .filter((level2Type) => typeof level2Type === 'string' && level2Type.startsWith(`${entityType}.`))
            .map((level2Type) => level2Type.slice(entityType.length + 1))
            .filter((subType) => Boolean(subType))
        : [];
      const mergedSubEntityTypes = apiSubEntityTypes.length > 0
        ? apiSubEntityTypes
        : configuredSubEntityTypes;
      let subEntitiesByEntityId = result?.subEntitiesByEntityId && typeof result.subEntitiesByEntityId === 'object'
        ? result.subEntitiesByEntityId
        : {};

      if (mergedSubEntityTypes.length > 0 && Object.keys(subEntitiesByEntityId).length === 0) {
        const allEntities = [...publicEntities, ...privateEntities];
        const loadedSubEntitiesByEntityId = {};

        await Promise.all(allEntities.map(async (entity) => {
          const mainEntityId = String(entity.id);
          const bySubType = {};

          await Promise.all(mergedSubEntityTypes.map(async (subEntityType) => {
            const combinedEntityType = `${entityType}.${subEntityType}`;
            try {
              const subResult = await getModuleSubEntities(module, combinedEntityType, mainEntityId);
              const items = Array.isArray(subResult?.items) ? subResult.items : [];
              if (items.length > 0) {
                bySubType[subEntityType] = items;
              }
            } catch (err) {
              // Keep loading resilient per sub-entity type.
            }
          }));

          if (Object.keys(bySubType).length > 0) {
            loadedSubEntitiesByEntityId[mainEntityId] = bySubType;
          }
        }));

        subEntitiesByEntityId = loadedSubEntitiesByEntityId;
      }

      const drafts = {};
      publicEntities.forEach((entity) => {
        drafts[`public:${String(entity.id)}`] = entity.description || '';
      });
      privateEntities.forEach((entity) => {
        drafts[`private:${String(entity.id)}`] = entity.description || '';
      });

      setEntityDialog({
        open: true,
        moduleName: module,
        entityType,
        loading: false,
        error: null,
        visibilitySupported: Boolean(result?.visibilitySupported),
        descriptionSupported: Boolean(result?.descriptionSupported),
        roles: Array.isArray(result?.roles) ? result.roles : [],
        subEntityTypes: mergedSubEntityTypes,
        subEntitiesByEntityId,
        publicEntities,
        privateEntities,
        drafts,
        originalDrafts: { ...drafts },
        savingDescriptionKey: null,
      });
    } catch (err) {
      setEntityDialog((previous) => ({
        ...previous,
        loading: false,
        error: err.message || 'Unable to load entities.',
      }));
    }
  }

  function closeEntityDialog() {
    setEntityDialog({
      open: false,
      moduleName: '',
      entityType: '',
      loading: false,
      error: null,
      visibilitySupported: false,
      descriptionSupported: false,
      roles: [],
      subEntityTypes: [],
      subEntitiesByEntityId: {},
      publicEntities: [],
      privateEntities: [],
      drafts: {},
      originalDrafts: {},
      savingDescriptionKey: null,
    });
  }

  async function refreshEntityDialog() {
    if (!entityDialog.moduleName || !entityDialog.entityType) {
      return;
    }
    await openEntityDialog(entityDialog.moduleName, entityDialog.entityType);
  }

  function setEntityDescriptionDraft(visibilityKey, entityId, value) {
    const key = `${visibilityKey}:${String(entityId)}`;
    setEntityDialog((previous) => ({
      ...previous,
      drafts: {
        ...previous.drafts,
        [key]: value,
      },
    }));
  }

  function isEntityDescriptionDirty(visibilityKey, entityId) {
    const key = `${visibilityKey}:${String(entityId)}`;
    const current = entityDialog.drafts[key] || '';
    const initial = entityDialog.originalDrafts[key] || '';
    return current !== initial;
  }

  function getSubEntities(mainEntityId, subEntityType) {
    const byMainEntity = entityDialog.subEntitiesByEntityId[String(mainEntityId)];
    if (!byMainEntity || typeof byMainEntity !== 'object') {
      return [];
    }
    const values = byMainEntity[subEntityType];
    return Array.isArray(values) ? values : [];
  }

  async function showSubEntityInfo(mainEntityId, subEntityType, subEntityId) {
    if (!entityDialog.moduleName || !entityDialog.entityType) {
      return;
    }

    try {
      setSaving(true);
      const combinedEntityType = `${entityDialog.entityType}.${subEntityType}`;
      const payloadEntityIds = [String(mainEntityId), String(subEntityId)];
      const info = await getModuleEntityInfo(entityDialog.moduleName, combinedEntityType, payloadEntityIds);
      window.alert(info || 'No additional information available.');
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Unable to load sub-entity information.' });
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteEntity(entityType, entityId, entityLabel) {
    if (!entityDialog.moduleName) {
      return;
    }

    const confirmed = window.confirm(`Do you really want to discard ${entityType} ${entityLabel}? This will delete all data it contains.`);
    if (!confirmed) {
      return;
    }

    try {
      setSaving(true);
      const removed = await removeModuleEntity(entityDialog.moduleName, entityType, [entityId]);
      if (!removed) {
        throw new Error(`Unable to discard ${entityLabel}.`);
      }
      setToast({ type: 'success', message: `${entityType} ${entityLabel} discarded.` });
      await refreshEntityDialog();
      await loadData();
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Unable to remove entity.' });
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleEntityVisibility(entityType, entityId, entityLabel, nextVisibility) {
    if (!entityDialog.moduleName) {
      return;
    }

    try {
      setSaving(true);
      const updated = await updateModuleEntityVisibility(entityDialog.moduleName, entityType, entityId, nextVisibility);
      if (!updated) {
        throw new Error(`Unable to set visibility for ${entityLabel}.`);
      }
      setToast({ type: 'success', message: `Visibility updated for ${entityLabel}.` });
      await refreshEntityDialog();
      await loadData();
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Unable to change visibility.' });
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveEntityDescription(entityType, visibilityKey, entityId, entityLabel) {
    if (!entityDialog.moduleName) {
      return;
    }

    const key = `${visibilityKey}:${String(entityId)}`;
    const description = entityDialog.drafts[key] || '';
    const confirmed = window.confirm(`Save description for ${entityType} ${entityLabel}?`);
    if (!confirmed) {
      return;
    }

    try {
      setEntityDialog((previous) => ({
        ...previous,
        savingDescriptionKey: key,
      }));
      const updated = await updateModuleEntityDescription(entityDialog.moduleName, entityType, entityId, description);
      if (!updated) {
        throw new Error(`Unable to update description for ${entityLabel}.`);
      }
      setEntityDialog((previous) => ({
        ...previous,
        originalDrafts: {
          ...previous.originalDrafts,
          [key]: description,
        },
      }));
      setToast({ type: 'success', message: `Description updated for ${entityLabel}.` });
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Unable to update description.' });
    } finally {
      setEntityDialog((previous) => ({
        ...previous,
        savingDescriptionKey: null,
      }));
    }
  }

  async function handleCreateDatabase(e) {
    e.preventDefault();
    if (!moduleName.trim()) {
      setToast({ type: 'warning', message: 'Database name is required.' });
      return;
    }
    if (!host) {
      setToast({ type: 'warning', message: 'Please select a host.' });
      return;
    }

    try {
      setSaving(true);
      const created = await createDatabase(moduleName.trim(), host);
      if (!created) {
        throw new Error('Database creation failed.');
      }
      setToast({ type: 'success', message: `Database "${moduleName.trim()}" created.` });
      setModuleName('');
      await loadData();
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteDatabase(name) {
    const confirmed = window.confirm(`Do you really want to delete database "${name}"?`);
    if (!confirmed) {
      return;
    }

    try {
      setSaving(true);
      const removed = await deleteDatabase(name, true);
      if (!removed) {
        throw new Error('Database deletion failed.');
      }
      setToast({ type: 'success', message: `Database "${name}" deleted.` });
      await loadData();
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  function markDirty(module, updater) {
    setEdits((prev) => {
      const current = prev[module];
      if (!current) {
        return prev;
      }
      const next = updater(current);
      return {
        ...prev,
        [module]: {
          ...next,
          dirty: true,
        },
      };
    });
  }

  async function handleCategoryCellClick(module) {
    if (!customization?.onCategoryFieldClick) {
      return;
    }

    const edit = edits[module];
    const details = databases[module];
    if (!edit || !details) {
      return;
    }

    try {
      const update = await customization.onCategoryFieldClick({
        moduleName: module,
        details,
        edit,
      });

      if (update && typeof update === 'object') {
        markDirty(module, (current) => ({
          ...current,
          ...update,
        }));
      }
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Unable to update category field.' });
    }
  }

  function resetModuleChanges(module) {
    const details = databases[module];
    if (!details) {
      return;
    }

    const parsed = parseCategoryWithCustomization(module, details);
    setEdits((prev) => ({
      ...prev,
      [module]: {
        categoryLabel: parsed.categoryLabel,
        categoryReadOnly: Boolean(parsed.categoryReadOnly),
        categoryTitle: parsed.categoryTitle || '',
        public: Boolean(details.public),
        hidden: Boolean(details.hidden),
        dirty: false,
      },
    }));
  }

  async function saveModuleChanges(module) {
    const edit = edits[module];
    if (!edit) {
      return;
    }

    try {
      setSaving(true);
      const categoryValue = buildCategoryValue(module, databases[module], edit);
      const updated = await updateDatabaseDetails(module, edit.public, edit.hidden, categoryValue);
      if (!updated) {
        throw new Error(`Unable to apply changes for ${module}`);
      }

      setDatabases((prev) => ({
        ...prev,
        [module]: {
          ...prev[module],
          public: edit.public,
          hidden: edit.hidden,
          category: categoryValue,
        },
      }));

      setEdits((prev) => ({
        ...prev,
        [module]: {
          ...prev[module],
          dirty: false,
        },
      }));
      setToast({ type: 'success', message: `Changes saved for ${module}.` });
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function openDumpDialog(module) {
    try {
      setDumpDialog({
        open: true,
        moduleName: module,
        loading: true,
        error: null,
        dumps: [],
        locked: false,
      });
      setRestoreCandidate(null);
      setRestoreWarning('');
      setDropBeforeRestore(true);
      const dumpInfo = await getDatabaseDumpInfo(module);

      const dateTag = new Date();
      const defaultDumpName = `${module}_${dateTag.getFullYear()}${String(dateTag.getMonth() + 1).padStart(2, '0')}${String(dateTag.getDate()).padStart(2, '0')}_${String(dateTag.getHours()).padStart(2, '0')}${String(dateTag.getMinutes()).padStart(2, '0')}${String(dateTag.getSeconds()).padStart(2, '0')}`;
      setNewDumpName(defaultDumpName);
      setNewDumpDescription('');

      setDumpDialog({
        open: true,
        moduleName: module,
        loading: false,
        error: null,
        dumps: Array.isArray(dumpInfo.dumps) ? dumpInfo.dumps : [],
        locked: Boolean(dumpInfo.locked),
      });
    } catch (err) {
      setDumpDialog({
        open: true,
        moduleName: module,
        loading: false,
        error: err.message,
        dumps: [],
        locked: false,
      });
    }
  }

  async function refreshModuleDumpStatus(module) {
    if (!module) {
      return;
    }

    try {
      const databasePayload = await listDatabases();
      const normalizedDatabases = databasePayload?.modules || databasePayload?.data || databasePayload || {};
      const latestDetails = normalizedDatabases && typeof normalizedDatabases === 'object' ? normalizedDatabases[module] : null;

      if (!latestDetails || !Object.prototype.hasOwnProperty.call(latestDetails, 'dumpStatus')) {
        return;
      }

      setDatabases((previous) => {
        if (!previous[module]) {
          return previous;
        }
        return {
          ...previous,
          [module]: {
            ...previous[module],
            dumpStatus: latestDetails.dumpStatus,
          },
        };
      });
    } catch (err) {
      console.warn(`Unable to refresh dump status for ${module}:`, err);
    }
  }

  function closeDumpDialog() {
    const module = dumpDialog.moduleName;

    setDumpDialog({
      open: false,
      moduleName: null,
      loading: false,
      error: null,
      dumps: [],
      locked: false,
    });
    setRestoreCandidate(null);
    setRestoreWarning('');

    if (module) {
      refreshModuleDumpStatus(module);
    }
  }

  async function refreshDumpDialog() {
    if (!dumpDialog.moduleName) {
      return;
    }
    await openDumpDialog(dumpDialog.moduleName);
  }

  async function handleDeleteDump(dumpIdentifier, dumpName) {
    const module = dumpDialog.moduleName;
    if (!module) {
      return;
    }

    const confirmed = window.confirm(`Delete dump ${dumpName} of database ${module}?`);
    if (!confirmed) {
      return;
    }

    try {
      setSaving(true);
      const result = await deleteDatabaseDump(module, dumpIdentifier);
      if (!result?.done) {
        throw new Error('Dump deletion failed.');
      }
      setToast({ type: 'success', message: `Dump ${dumpName} deleted.` });
      await refreshDumpDialog();
      await loadData();
    } catch (err) {
      setToast({ type: 'error', message: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function prepareRestore(dump) {
    const module = dumpDialog.moduleName;
    if (!module) {
      return;
    }

    setRestoreCandidate(dump);
    try {
      const warning = await getDumpRestoreWarning(module, dump.identifier);
      setRestoreWarning(warning || 'Restoring this dump seems safe.');
    } catch (err) {
      setRestoreWarning(`Unable to get restore warning: ${err.message}`);
    }
  }

  function startDumpProcess() {
    const module = dumpDialog.moduleName;
    if (!module) {
      return;
    }

    const url = getNewDumpUrl(module, newDumpName || module, newDumpDescription);
    window.open(url, '_blank', 'noopener');
    setToast({ type: 'info', message: 'Dump process started in a new tab.' });
  }

  async function handleCustomAction(action, row) {
    try {
      setSaving(true);
      await action.onClick(row, {
        reload: loadData,
        openDumpDialog,
        saveModuleChanges,
        isAdmin,
        canCreateDatabases,
      });
      if (action.refreshAfterAction !== false) {
        await loadData();
      }
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Action failed.' });
    } finally {
      setSaving(false);
    }
  }

  function getRowContext(name, details) {
    const context = { name, details };
    if (!customization?.mapRow) {
      return context;
    }

    try {
      const mapped = customization.mapRow(context);
      if (mapped && mapped.name && mapped.details) {
        return mapped;
      }
    } catch (err) {
      console.warn('Unable to apply database row customization:', err);
    }

    return context;
  }

  function getCustomActions(rowContext) {
    if (!customization?.getRowActions) {
      return [];
    }

    try {
      const actions = customization.getRowActions(rowContext, {
        isAdmin,
        canCreateDatabases,
      });
      return Array.isArray(actions) ? actions : [];
    } catch (err) {
      console.warn('Unable to apply custom row actions:', err);
      return [];
    }
  }

  function getTaxonomySearchName(details) {
    const rawCategory = details?.category || '';
    const parts = rawCategory.split(':');
    if (parts.length >= 3) {
      return parts[2] || parts[1] || parts[0] || '';
    }
    return rawCategory;
  }

  return (
    <div className="role-manager-container database-manager-page">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-hdd-stack me-2"></i>
          Manage Databases
        </h2>
        <div className="d-flex gap-2 flex-wrap justify-content-end">
          {pageLinks.map((link) => (
            link.href && isSameOriginUrl(typeof link.href === 'function' ? link.href({ isAdmin, canCreateDatabases }) : link.href) ? (
              <button
                key={link.key || link.label}
                type="button"
                className={`btn btn-sm ${link.className || 'btn-outline-primary'}`}
                onClick={() => {
                  const href = typeof link.href === 'function' ? link.href({ isAdmin, canCreateDatabases }) : link.href;
                  openContentDialog(href, link.title || link.label);
                }}
                title={link.title || link.label}
              >
                {link.icon && <i className={`${link.icon} me-1`}></i>}
                {link.label}
              </button>
            ) : link.href ? (
              <a
                key={link.key || link.label}
                className={`btn btn-sm ${link.className || 'btn-outline-primary'}`}
                href={typeof link.href === 'function' ? link.href({ isAdmin, canCreateDatabases }) : link.href}
                target={link.target || '_blank'}
                rel="noreferrer"
                title={link.title || link.label}
              >
                {link.icon && <i className={`${link.icon} me-1`}></i>}
                {link.label}
              </a>
            ) : null
          ))}
          <button className="btn btn-outline-secondary" onClick={() => loadData()} disabled={loading || saving}>
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh
          </button>
        </div>
      </div>

      {canCreateDatabases && (
        <div className="card mb-4">
          <div className="card-body">
            <h5 className="card-title mb-3">Create New Database</h5>
            <form className="row g-2" onSubmit={handleCreateDatabase}>
              <div className="col-md-6">
                <input
                  type="text"
                  className="form-control"
                  placeholder="Database name"
                  value={moduleName}
                  onChange={(e) => setModuleName(e.target.value)}
                  disabled={saving}
                />
              </div>
              <div className="col-md-4">
                <select
                  className="form-select"
                  value={host}
                  onChange={(e) => setHost(e.target.value)}
                  disabled={saving || hosts.length === 0}
                >
                  {hosts.length === 0 && <option value="">No host available</option>}
                  {hosts.map((h) => (
                    <option key={h} value={h}>
                      {h}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-md-2 d-grid">
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {error && (
        <div className="alert alert-danger" role="alert">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </div>
      )}

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover mb-0 database-main-table">
            <thead>
              <tr>
                <th>{columnLabels.database}</th>
                <th>{columnLabels.host}</th>
                <th>{columnLabels.category}</th>
                <th>{columnLabels.size}</th>
                <th className="entity-management-column">{columnLabels.entityManagement}</th>
                <th>{columnLabels.public}</th>
                <th>{columnLabels.hidden}</th>
                {dumpManagementEnabled && <th>{columnLabels.dumpStatus}</th>}
                <th style={{ width: dumpManagementEnabled ? '320px' : '260px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={dumpManagementEnabled ? '9' : '8'} className="text-center py-4">
                    <span className="spinner-border spinner-border-sm text-primary me-2" role="status" aria-hidden="true"></span>
                    Loading databases...
                  </td>
                </tr>
              ) : databaseEntries.length === 0 ? (
                <tr>
                  <td colSpan={dumpManagementEnabled ? '9' : '8'} className="text-center text-muted py-4">
                    No database available.
                  </td>
                </tr>
              ) : (
                databaseEntries.map(([name, details]) => {
                  const rowContext = getRowContext(name, details);
                  const customActions = getCustomActions(rowContext);
                  const edit = edits[name];
                  const editable = canEditModule(currentUser, isAdmin, name);

                  return (
                    <tr key={name} className={`database-row ${edit?.dirty ? 'database-row-dirty' : ''}`}>
                      <td className="fw-medium">{rowContext.name}</td>
                      <td>{rowContext.details.host || '-'}</td>
                      <td>
                        {editable && edit ? (
                          <div className="d-flex gap-1 align-items-center">
                            {edit.categoryReadOnly ? (
                              <input
                                readonly="readonly"
                                type="text"
                                className="btn btn-sm btn-outline-secondary form-control form-control-sm"
                                value={edit.categoryLabel}
                                onChange={(e) => markDirty(name, (current) => ({ ...current, categoryLabel: e.target.value }))}
                                style={{ width: '200px' }}
                                title={edit.categoryTitle || 'Select category value'}
                                onClick={() => handleCategoryCellClick(name)}
                              />
                            ) : (
                              <input
                                type="text"
                                className="form-control form-control-sm"
                                value={edit.categoryLabel}
                                onChange={(e) => markDirty(name, (current) => ({ ...current, categoryLabel: e.target.value }))}
                                style={{ width: '200px' }}
                              />
                            )}
                          </div>
                        ) : (
                          <span>{edit?.categoryLabel || rowContext.details.category || '-'}</span>
                        )}
                      </td>
                      <td>{formatSize(rowContext.details.size)}</td>
                      <td className="entity-management-cell">
                        <div className="d-flex gap-1">
                          {level1Types.map((entityType) => (
                            <button
                              key={`${name}-${entityType}`}
                              className="btn btn-sm btn-outline-secondary text-nowrap"
                              type="button"
                              onClick={() => openEntityDialog(rowContext.name, entityType)}
                              title={`Manage ${entityType} entities`}
                            >
                              {entityType} entities
                            </button>
                          ))}
                          {level1Types.length === 0 && <span className="text-muted">-</span>}
                        </div>
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          className="form-check-input"
                          checked={Boolean(edit?.public)}
                          disabled={!editable}
                          onChange={(e) => markDirty(name, (current) => ({ ...current, public: e.target.checked }))}
                        />
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          className="form-check-input"
                          checked={Boolean(edit?.hidden)}
                          disabled={!editable}
                          onChange={(e) => markDirty(name, (current) => ({ ...current, hidden: e.target.checked }))}
                        />
                      </td>
                      {dumpManagementEnabled && (() => {
                        const dumpStatus = String(rowContext.details.dumpStatus || '').toUpperCase();
                        return (
                          <td
                            className={`dump-status-cell dump-status-cell-clickable ${getDumpValidityClass(dumpStatus)}`}
                            title={dumpValidityTips[dumpStatus] || 'Open dump management'}
                            onClick={() => openDumpDialog(name)}
                            onKeyDown={(event) => {
                              if (event.key === 'Enter' || event.key === ' ') {
                                event.preventDefault();
                                openDumpDialog(name);
                              }
                            }}
                            role="button"
                            tabIndex={0}
                            aria-label={`Open dump management for ${name}`}
                          >
                            <span className="dump-status-link">{dumpStatus ? dumpStatus.toLowerCase() : '-'}</span>
                          </td>
                        );
                      })()}
                      <td>
                        <div className="d-flex gap-1 flex-wrap">
                          {editable && (
                            <>
                              <button
                                className={`btn btn-sm ${edit?.dirty ? 'btn-success pending-save-btn' : 'btn-outline-success'}`}
                                onClick={() => saveModuleChanges(name)}
                                disabled={saving || !edit?.dirty}
                                title="Save changes"
                                type="button"
                              >
                                <i className="bi bi-check2"></i>
                              </button>
                              <button
                                className={`btn btn-sm ${edit?.dirty ? 'btn-warning pending-reset-btn' : 'btn-outline-secondary'}`}
                                onClick={() => resetModuleChanges(name)}
                                disabled={saving || !edit?.dirty}
                                title="Reset changes"
                                type="button"
                              >
                                <i className="bi bi-arrow-counterclockwise"></i>
                              </button>
                              {/* {dumpManagementEnabled && (
                                <button
                                  className="btn btn-sm btn-outline-primary"
                                  onClick={() => openDumpDialog(name)}
                                  disabled={saving}
                                  title="Manage dumps"
                                  type="button"
                                >
                                  <i className="bi bi-archive"></i>
                                </button>
                              )} */}
                            </>
                          )}
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => handleDeleteDatabase(rowContext.name)}
                            disabled={saving || !editable}
                            title="Delete database"
                            type="button"
                          >
                            <i className="bi bi-trash"></i>
                          </button>
                          {customActions.map((action) => (
                            action.href && isSameOriginUrl(typeof action.href === 'function' ? action.href(rowContext) : action.href) ? (
                              <button
                                key={`${name}-${action.key || action.label}`}
                                className={`btn btn-sm ${action.className || 'btn-outline-secondary'}`}
                                type="button"
                                onClick={() => {
                                  const href = typeof action.href === 'function' ? action.href(rowContext) : action.href;
                                  openContentDialog(href, action.title || action.label);
                                }}
                                title={action.title || action.label}
                              >
                                {action.icon && <i className={`${action.icon} me-1`}></i>}
                                {action.label}
                              </button>
                            ) : action.href ? (
                              <a
                                key={`${name}-${action.key || action.label}`}
                                className={`btn btn-sm ${action.className || 'btn-outline-secondary'}`}
                                href={typeof action.href === 'function' ? action.href(rowContext) : action.href}
                                target={action.target || '_blank'}
                                rel="noreferrer"
                                title={action.title || action.label}
                              >
                                {action.icon && <i className={`${action.icon} me-1`}></i>}
                                {action.label}
                              </a>
                            ) : (
                              <button
                                key={`${name}-${action.key || action.label}`}
                                className={`btn btn-sm ${action.className || 'btn-outline-secondary'}`}
                                onClick={() => handleCustomAction(action, rowContext)}
                                disabled={saving || action.disabled === true}
                                title={action.title || action.label}
                                type="button"
                              >
                                {action.icon && <i className={`${action.icon} me-1`}></i>}
                                {action.label}
                              </button>
                            )
                          ))}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {dumpManagementEnabled && dumpDialog.open && (
        <div className="dump-management-backdrop" onClick={closeDumpDialog} role="presentation">
          <div className="card dump-management-modal border-primary shadow-lg" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label={`Dump management for ${dumpDialog.moduleName || ''}`}>
            <div className="card-header d-flex justify-content-between align-items-center">
              <strong>Dump management for {dumpDialog.moduleName}</strong>
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={closeDumpDialog}>Close</button>
            </div>
            <div className="card-body">
            {dumpDialog.error && <div className="alert alert-danger">{dumpDialog.error}</div>}
            {dumpDialog.loading ? (
              <div className="text-muted">Loading dumps...</div>
            ) : (
              <>
                {dumpDialog.locked && (
                  <div className="alert alert-warning">
                    This module is busy, dump operations cannot be performed at the moment.
                  </div>
                )}

                <div className="table-responsive">
                  <table className="table table-sm table-striped database-dumps-table">
                    <thead>
                      <tr>
                        <th>Validity</th>
                        <th>Dump Name</th>
                        <th>Archive Size</th>
                        <th>Creation Date</th>
                        <th>Description</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dumpDialog.dumps.length === 0 ? (
                        <tr>
                          <td colSpan="6" className="text-muted">No existing dump.</td>
                        </tr>
                      ) : (
                        dumpDialog.dumps.map((dump) => {
                          const downloadDisabled = dump.validity === 'NONE' || dump.validity === 'BUSY';
                          const validity = String(dump.validity || '').toUpperCase();
                          return (
                            <tr key={dump.identifier}>
                              <td className={`dump-status-cell ${getDumpValidityClass(validity)}`} title={dumpValidityTips[validity] || ''}>{validity ? validity.toLowerCase() : '-'}</td>
                              <td>{dump.name}</td>
                              <td>{formatSize(dump.fileSizeMb)}</td>
                              <td>{new Date(dump.creationDate).toLocaleString()}</td>
                              <td style={{ whiteSpace: 'pre-wrap' }}>{dump.description || ''}</td>
                              <td>
                                <div className="d-flex gap-1 flex-wrap">
                                  {!downloadDisabled && (
                                    <a className="btn btn-sm btn-outline-secondary" target="_blank" rel="noreferrer" href={getDumpDownloadUrl(dumpDialog.moduleName, dump.identifier)}>
                                      Download
                                    </a>
                                  )}
                                  {!downloadDisabled && (
                                    <a className="btn btn-sm btn-outline-secondary" target="_blank" rel="noreferrer" href={getDumpLogDownloadUrl(dumpDialog.moduleName, dump.identifier)}>
                                      Logs
                                    </a>
                                  )}
                                  {!dumpDialog.locked && (
                                    <button className="btn btn-sm btn-outline-primary" type="button" onClick={() => prepareRestore(dump)}>
                                      Restore
                                    </button>
                                  )}
                                  {!dumpDialog.locked && (
                                    <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => handleDeleteDump(dump.identifier, dump.name)}>
                                      Delete
                                    </button>
                                  )}
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>

                {!dumpDialog.locked && (
                  <div className="mt-3 border-top pt-3">
                    <h6>New dump</h6>
                    <div className="row g-2 align-items-start">
                      <div className="col-md-4">
                        <label className="form-label">Dump name</label>
                        <input className="form-control form-control-sm" value={newDumpName} onChange={(e) => setNewDumpName(e.target.value)} />
                      </div>
                      <div className="col-md-6">
                        <label className="form-label">Description</label>
                        <textarea className="form-control form-control-sm" rows="2" value={newDumpDescription} onChange={(e) => setNewDumpDescription(e.target.value)} />
                      </div>
                      <div className="col-md-2 d-grid">
                        <label className="form-label">&nbsp;</label>
                        <button type="button" className="btn btn-sm btn-danger" onClick={startDumpProcess}>Start dump</button>
                      </div>
                    </div>
                  </div>
                )}

                {restoreCandidate && (
                  <div className="mt-3 border-top pt-3">
                    <h6>Restore dump {restoreCandidate.name}</h6>
                    <p className="small mb-2">
                      {restoreWarning ? restoreWarning : 'Checking restore safety...'}
                    </p>
                    <div className="form-check mb-2">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="dropBeforeRestore"
                        checked={dropBeforeRestore}
                        onChange={(e) => setDropBeforeRestore(e.target.checked)}
                      />
                      <label className="form-check-label" htmlFor="dropBeforeRestore">
                        Drop the database before restoring
                      </label>
                    </div>
                    <div className="d-flex gap-2">
                      <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setRestoreCandidate(null)}>
                        Cancel
                      </button>
                      <a
                        className="btn btn-sm btn-danger"
                        href={getRestoreDumpUrl(dumpDialog.moduleName, restoreCandidate.identifier, dropBeforeRestore)}
                        target="_blank"
                        rel="noreferrer"
                        onClick={() => setRestoreCandidate(null)}
                      >
                        Confirm restore
                      </a>
                    </div>
                  </div>
                )}
              </>
            )}
            </div>
          </div>
        </div>
      )}

      {contentDialog.open && (
        <div className="dump-management-backdrop" onClick={closeContentDialog} role="presentation">
          <div className="card dump-management-modal border-primary shadow-lg" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label={contentDialog.title || 'Content dialog'}>
            <div className="card-header d-flex justify-content-between align-items-center">
              <strong>{contentDialog.title || 'Details'}</strong>
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={closeContentDialog}>Close</button>
            </div>
            <div className="card-body p-2">
              <iframe
                title={contentDialog.title || 'Content'}
                src={contentDialog.url}
                className="w-100 border-0"
                style={{ minHeight: '70vh' }}
              ></iframe>
            </div>
          </div>
        </div>
      )}

      {entityDialog.open && (
        <div className="dump-management-backdrop" onClick={closeEntityDialog} role="presentation">
          <div
            className="card dump-management-modal border-primary shadow-lg"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label={`Entity management for ${entityDialog.moduleName || ''}`}
          >
            <div className="card-header d-flex justify-content-between align-items-center">
              <strong>
                {entityDialog.entityType} entities for user <u>{currentUser.username}</u> in database <u>{entityDialog.moduleName}</u>
              </strong>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => refreshEntityDialog()}
                  disabled={entityDialog.loading}
                >
                  Refresh
                </button>
                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={closeEntityDialog}>Close</button>
              </div>
            </div>
            <div className="card-body">
              {entityDialog.error && <div className="alert alert-danger mb-3">{entityDialog.error}</div>}
              {entityDialog.loading ? (
                <div className="text-muted">Loading entities...</div>
              ) : (
                <>
                  {/* <div className="small text-muted mb-3">
                    {entityDialog.roles.length > 0
                      ? `Roles: ${entityDialog.roles.join(', ')}`
                      : 'No role information available for this entity type.'}
                  </div> */}

                  <div className="table-responsive mb-3">
                    <table className="table table-sm table-striped mb-0">
                      <thead>
                        <tr>
                          <th>Visibility</th>
                          <th>ID</th>
                          <th>Label</th>
                          {entityDialog.subEntityTypes.map((subEntityType) => (
                            <th key={`sub-entity-header-${subEntityType}`}>{subEntityType} sub-entities</th>
                          ))}
                          {entityDialog.descriptionSupported && <th>Description</th>}
                          {entityDialog.visibilitySupported && <th>Public</th>}
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {entityDialog.publicEntities.map((entity) => (
                          <tr key={`public-${String(entity.id)}`}>
                            <td>Public</td>
                            <td>{String(entity.id)}</td>
                            <td>{entity.label || '-'}</td>
                            {entityDialog.subEntityTypes.map((subEntityType) => {
                              const subEntities = getSubEntities(entity.id, subEntityType);
                              return (
                                <td key={`public-${String(entity.id)}-${subEntityType}`}>
                                  <div className="d-flex gap-1 flex-wrap">
                                    {subEntities.length === 0 ? (
                                      <span className="text-muted">-</span>
                                    ) : (
                                      subEntities.map((subEntity) => (
                                        <button
                                          key={`public-${String(entity.id)}-${subEntityType}-${String(subEntity.id)}`}
                                          type="button"
                                          className="btn btn-sm btn-outline-info"
                                          onClick={() => showSubEntityInfo(entity.id, subEntityType, subEntity.id)}
                                          disabled={saving}
                                          title={`Show info for ${subEntityType} ${subEntity.label || String(subEntity.id)}`}
                                        >
                                          {subEntity.label || String(subEntity.id)}
                                        </button>
                                      ))
                                    )}
                                  </div>
                                </td>
                              );
                            })}
                            {entityDialog.descriptionSupported && (
                              <td>
                                <div className="d-flex gap-1">
                                  <textarea
                                    className="form-control form-control-sm"
                                    rows="2"
                                    value={entityDialog.drafts[`public:${String(entity.id)}`] || ''}
                                    onChange={(event) => setEntityDescriptionDraft('public', entity.id, event.target.value)}
                                    disabled={saving}
                                  />
                                  <button
                                    type="button"
                                    className={`btn btn-sm ${isEntityDescriptionDirty('public', entity.id) ? 'btn-success' : 'btn-outline-secondary'}`}
                                    onClick={() => handleSaveEntityDescription(entityDialog.entityType, 'public', entity.id, entity.label || String(entity.id))}
                                    disabled={entityDialog.savingDescriptionKey === `public:${String(entity.id)}` || !isEntityDescriptionDirty('public', entity.id)}
                                  >
                                    Save
                                  </button>
                                </div>
                              </td>
                            )}
                            {entityDialog.visibilitySupported && (
                              <td className="text-center align-middle">
                                <input
                                  type="checkbox"
                                  className="form-check-input"
                                  checked
                                  disabled={saving}
                                  onChange={(event) => handleToggleEntityVisibility(entityDialog.entityType, entity.id, entity.label || String(entity.id), event.target.checked)}
                                />
                              </td>
                            )}
                            <td className="align-middle">
                              <div className="d-flex gap-1 flex-wrap">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-danger"
                                  onClick={() => handleDeleteEntity(entityDialog.entityType, entity.id, entity.label || String(entity.id))}
                                  disabled={saving}
                                >
                                  Delete
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {entityDialog.visibilitySupported && entityDialog.privateEntities.map((entity) => (
                          <tr key={`private-${String(entity.id)}`}>
                            <td>Private</td>
                            <td>{String(entity.id)}</td>
                            <td>{entity.label || '-'}</td>
                            {entityDialog.subEntityTypes.map((subEntityType) => {
                              const subEntities = getSubEntities(entity.id, subEntityType);
                              return (
                                <td key={`private-${String(entity.id)}-${subEntityType}`}>
                                  <div className="d-flex gap-1 flex-wrap">
                                    {subEntities.length === 0 ? (
                                      <span className="text-muted">-</span>
                                    ) : (
                                      subEntities.map((subEntity) => (
                                        <button
                                          key={`private-${String(entity.id)}-${subEntityType}-${String(subEntity.id)}`}
                                          type="button"
                                          className="btn btn-sm btn-outline-info"
                                          onClick={() => showSubEntityInfo(entity.id, subEntityType, subEntity.id)}
                                          disabled={saving}
                                          title={`Show info for ${subEntityType} ${subEntity.label || String(subEntity.id)}`}
                                        >
                                          {subEntity.label || String(subEntity.id)}
                                        </button>
                                      ))
                                    )}
                                  </div>
                                </td>
                              );
                            })}
                            {entityDialog.descriptionSupported && (
                              <td>
                                <div className="d-flex gap-1">
                                  <textarea
                                    className="form-control form-control-sm"
                                    rows="2"
                                    value={entityDialog.drafts[`private:${String(entity.id)}`] || ''}
                                    onChange={(event) => setEntityDescriptionDraft('private', entity.id, event.target.value)}
                                    disabled={saving}
                                  />
                                  <button
                                    type="button"
                                    className={`btn btn-sm ${isEntityDescriptionDirty('private', entity.id) ? 'btn-success' : 'btn-outline-secondary'}`}
                                    onClick={() => handleSaveEntityDescription(entityDialog.entityType, 'private', entity.id, entity.label || String(entity.id))}
                                    disabled={entityDialog.savingDescriptionKey === `private:${String(entity.id)}` || !isEntityDescriptionDirty('private', entity.id)}
                                  >
                                    Save
                                  </button>
                                </div>
                              </td>
                            )}
                            {entityDialog.visibilitySupported && (
                              <td className="text-center align-middle">
                                <input
                                  type="checkbox"
                                  className="form-check-input"
                                  checked={false}
                                  disabled={saving}
                                  onChange={(event) => handleToggleEntityVisibility(entityDialog.entityType, entity.id, entity.label || String(entity.id), event.target.checked)}
                                />
                              </td>
                            )}
                            <td className="align-middle">
                              <div className="d-flex gap-1 flex-wrap">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-danger"
                                  onClick={() => handleDeleteEntity(entityDialog.entityType, entity.id, entity.label || String(entity.id))}
                                  disabled={saving}
                                >
                                  Delete
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {entityDialog.publicEntities.length === 0 && (!entityDialog.visibilitySupported || entityDialog.privateEntities.length === 0) && (
                          <tr>
                            <td
                              colSpan={
                                (entityDialog.descriptionSupported ? 1 : 0) +
                                (entityDialog.visibilitySupported ? 1 : 0) +
                                entityDialog.subEntityTypes.length +
                                4
                              }
                              className="text-muted"
                            >
                              No entities found for this module and entity type.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {toast && <Toast type={toast.type} message={toast.message} onClose={() => setToast(null)} />}
    </div>
  );
}
