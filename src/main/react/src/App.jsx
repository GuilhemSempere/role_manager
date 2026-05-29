import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { RoleConfigProvider } from './context/RoleConfigContext';
import Layout from './components/Layout';
import UserList from './components/UserList';
import UserDetails from './components/UserDetails';
import UserPermissions from './components/UserPermissions';
import DatabaseManager from './components/DatabaseManager';

function App() {
  return (
    <AuthProvider>
      <RoleConfigProvider>
        <Layout>
          <Routes>
            <Route path="/" element={<UserList />} />
            <Route path="/users" element={<UserList />} />
            <Route path="/user/new" element={<UserDetails isNew={true} />} />
            <Route path="/user/:username" element={<UserDetails />} />
            <Route path="/user/:username/clone" element={<UserDetails isClone={true} />} />
            <Route path="/user/:username/permissions/:module/:entityType" element={<UserPermissions />} />
            <Route path="/databases" element={<DatabaseManager />} />
          </Routes>
        </Layout>
      </RoleConfigProvider>
    </AuthProvider>
  );
}

export default App;
