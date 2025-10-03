import React, { useState, useEffect } from 'react';
import { Auth } from 'aws-amplify';
import AuthComponent from './components/Auth.jsx';
import TaskForm from './components/TaskForm.jsx';
import TaskList from './components/TaskList.jsx';
import todoApi from './services/todoApi.js';
import './App.css';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    checkAuthState();
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      loadTasks();
    }
  }, [isAuthenticated]);

  const checkAuthState = async () => {
    try {
      const user = await Auth.currentAuthenticatedUser();
      console.log('User authenticated successfully:', user);
      setIsAuthenticated(true);
    } catch (error) {
      if (error === 'The user is not authenticated') {
        // console.log('Authentication check error, retrying...', error);
        setTimeout(checkAuthState, 1000);
        // setIsAuthenticated(false);
      } 
    }
  };

  const loadTasks = async () => {
    try {
      setIsLoading(true);
      const tasksData = await todoApi.getTasks();
      setTasks(tasksData);
      setError('');
    } catch (error) {
      console.error('Error loading tasks:', error);
      setError('Failed to load tasks. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateTask = async (taskData) => {
    try {
      setIsLoading(true);
      await todoApi.createTask(taskData);
      await loadTasks();
      setError('');
    } catch (error) {
      console.error('Error creating task:', error);
      setError('Failed to create task. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateTask = async (taskId, updates) => {
    try {
      await todoApi.updateTask(taskId, updates);
      await loadTasks(); // Reload tasks to reflect changes
      setError('');
    } catch (error) {
      console.error('Error updating task:', error);
      setError('Failed to update task. Please try again.');
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await todoApi.deleteTask(taskId);
        await loadTasks(); // Reload tasks after deletion
        setError('');
      } catch (error) {
        console.error('Error deleting task:', error);
        setError('Failed to delete task. Please try again.');
      }
    }
  };

  const handleAuthStateChange = (authenticated) => {
    setIsAuthenticated(authenticated);
    if (authenticated) {
      setError('');
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="app">
        <div className="app-header">
          <h1>To-Do Application</h1>
          {/* <p>Please sign in to manage your tasks</p> */}
        </div>
        <AuthComponent onAuthStateChange={handleAuthStateChange} />
      </div>
    );
  }

  return (
    <div className="app">
      <div className="app-header">
        <h1>To-Do Application</h1>
        <p>Manage your tasks efficiently</p>
      </div>

      {error && (
        <div className="error-message">
          {error}
          <button onClick={() => setError('')} className="error-close">×</button>
        </div>
      )}

      <div className="app-content">
        <TaskForm onSubmit={handleCreateTask} isLoading={isLoading} />
        
        <div className="tasks-section">
          <div className="section-header">
            <h2>Your Tasks ({tasks.length})</h2>
            <button onClick={loadTasks} className="refresh-btn" disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Refresh'}
            </button>
          </div>
          
          {isLoading && tasks.length === 0 ? (
            <div className="loading">Loading tasks...</div>
          ) : (
            <TaskList
              tasks={tasks}
              onUpdateTask={handleUpdateTask}
              onDeleteTask={handleDeleteTask}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export default App;