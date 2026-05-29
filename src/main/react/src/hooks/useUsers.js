import { useState, useEffect, useCallback } from 'react';
import { listUsers, countUsers } from '../api/roleManagerApi';

export function useUsers(initialSearch = '', pageSize = 20) {
  const [users, setUsers] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState(initialSearch);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadUsers = useCallback(async (search, page) => {
    try {
      setLoading(true);
      setError(null);
      
      const [usersData, count] = await Promise.all([
        listUsers(search, page, pageSize),
        countUsers(search),
      ]);
      
      // Transform the array format to objects
      // Format from backend: [[username, authoritySummary, method], ...]
      const transformedUsers = usersData.map(row => ({
        username: row[0],
        authoritySummary: row[1],
        method: row[2],
      }));
      
      setUsers(transformedUsers);
      setTotalCount(count);
    } catch (err) {
      setError(err.message);
      setUsers([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    loadUsers(searchTerm, currentPage);
  }, [searchTerm, currentPage, loadUsers]);

  function search(term) {
    setSearchTerm(term);
    setCurrentPage(0);
  }

  function goToPage(page) {
    setCurrentPage(page);
  }

  function refresh() {
    return loadUsers(searchTerm, currentPage);
  }

  const totalPages = Math.ceil(totalCount / pageSize);

  return {
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
  };
}
