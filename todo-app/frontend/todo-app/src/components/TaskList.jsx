import React from 'react';
import { format, isBefore } from 'date-fns';
import './TaskList.css';

const TaskList = ({ tasks, onUpdateTask, onDeleteTask }) => {
  const getStatusBadge = (task) => {
    const now = new Date();
    const deadline = new Date(task.deadline);

    if (task.status === 'Completed') {
      return <span className="status-badge completed">Completed</span>;
    } else if (task.status === 'Expired') {
      return <span className="status-badge expired">Expired</span>;
    } else if (isBefore(deadline, now)) {
      return <span className="status-badge expired">Expired</span>;
    } else {
      return <span className="status-badge pending">Pending</span>;
    }
  };

  const handleStatusChange = (taskId, currentStatus) => {
    const newStatus = currentStatus === 'Pending' ? 'Completed' : 'Pending';
    onUpdateTask(taskId, { status: newStatus });
  };

  if (tasks.length === 0) {
    return (
      <div className="empty-state">
        <p>No tasks found. Create your first task!</p>
      </div>
    );
  }

  return (
    <div className="task-list">
      {tasks.map((task) => (
        <div key={task.taskId} className="task-card">
          <div className="task-header">
            <h3 className="task-description">{task.description}</h3>
            {getStatusBadge(task)}
          </div>
          
          <div className="task-details">
            <div className="task-date">
              <strong>Date:</strong> {task.date}
            </div>
            <div className="task-deadline">
              <strong>Deadline:</strong> {format(new Date(task.deadline), 'MMM dd, yyyy HH:mm')}
            </div>
            <div className="task-created">
              <strong>Created:</strong> {format(new Date(task.createdAt), 'MMM dd, yyyy HH:mm')}
            </div>
          </div>

          <div className="task-actions">
            <button
              onClick={() => handleStatusChange(task.taskId, task.status)}
              className={`status-btn ${task.status === 'Completed' ? 'undo' : 'complete'}`}
            >
              {task.status === 'Completed' ? 'Mark Pending' : 'Mark Complete'}
            </button>
            
            <button
              onClick={() => onDeleteTask(task.taskId)}
              className="delete-btn"
            >
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
};

export default TaskList;