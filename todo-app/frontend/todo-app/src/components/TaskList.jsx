import React from 'react';
import { format, isBefore } from 'date-fns';
import './TaskList.css';

const safeDate = (value) => {
    if (value === null || value === undefined || value === '') return null;

    if (value instanceof Date) {
        return isNaN(value.getTime()) ? null : value;
    }

    if (typeof value === 'string') {
        const trimmed = value.trim();
        if (!trimmed) return null;
        if (/^-?\d+$/.test(trimmed)) {
            value = Number(trimmed);
        } else {
            const d = new Date(trimmed);
            return isNaN(d.getTime()) ? null : d;
        }
    }

    if (typeof value === 'number') {
        let n = value;
        const abs = Math.abs(n);
        if (abs < 1e12) {
            n *= 1000;
        } else if (abs >= 1e15 && abs < 1e17) {
            n = Math.floor(n / 1000);
        } else if (abs >= 1e17) {
            n = Math.floor(n / 1e6);
        }
        const d = new Date(n);
        return isNaN(d.getTime()) ? null : d;
    }

    return null;
};

const safeFormat = (date, formatString, fallback = 'N/A') => {
    if (!date || isNaN(date.getTime())) {
        return fallback;
    }
    try {
        return format(date, formatString);
    } catch (error) {
        console.warn('Date formatting error:', error);
        return fallback;
    }
};

const TaskList = ({ tasks, onUpdateTask, onDeleteTask }) => {
    const getStatusBadge = (task) => {
        const now = new Date();
        const deadlineDate = safeDate(task.deadline);

        if (task.status === 'Completed') {
            return <span className="status-badge completed">Completed</span>;
        } else if (task.status === 'Expired') {
            return <span className="status-badge expired">Expired</span>;
        } else if (deadlineDate && isBefore(deadlineDate, now)) {
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
            {tasks.map((task) => {
                const deadlineDate = safeDate(task.deadline);
                const createdAtDate = safeDate(task.createdAt);

                return (
                    <div key={task.taskId} className="task-card">
                        <div className="task-header">
                            <h3 className="task-description">{task.description}</h3>
                            {getStatusBadge(task)}
                        </div>

                        <div className="task-details">
                            <div className="task-date">
                                <strong>Date:</strong> {task.date || 'N/A'}
                            </div>
                            <div className="task-deadline">
                                <strong>Deadline:</strong> {safeFormat(deadlineDate, 'MMM dd, yyyy HH:mm')}
                            </div>
                            <div className="task-created">
                                <strong>Created:</strong> {safeFormat(createdAtDate, 'MMM dd, yyyy HH:mm')}
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
                );
            })}
        </div>
    );
};

export default TaskList;