import { createContext, useContext, useState, useEffect } from 'react';
import { getCurrentUser } from '../api/roleManagerApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadCurrentUser();
  }, []);

  async function loadCurrentUser() {
    try {
      setLoading(true);
      const user = await getCurrentUser();
      setCurrentUser(user);
      setError(null);
    } catch (err) {
      setError(err.message);
      setCurrentUser(null);
    } finally {
      setLoading(false);
    }
  }

  function refresh() {
    return loadCurrentUser();
  }

  const value = {
    currentUser,
    loading,
    error,
    refresh,
    isAuthenticated: currentUser?.authenticated === true,
    isAdmin: currentUser?.isAdmin === true,
    canWriteToSystem: currentUser?.canWriteToSystem === true,
    supervisedModules: currentUser?.supervisedModules || [],
    managedEntities: currentUser?.managedEntities || {},
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
