import React, { useState } from 'react';
import { format } from 'date-fns';
import './TaskForm.css';

const TaskForm = ({ onSubmit, isLoading }) => {
  const [formData, setFormData] = useState({
    description: '',
    date: format(new Date(), 'yyyy-MM-dd')
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formData.description.trim()) {
      onSubmit(formData);
      setFormData({
        description: '',
        date: format(new Date(), 'yyyy-MM-dd')
      });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="task-form">
      <h3>Create New Task</h3>
      
      <div className="form-group">
        <label htmlFor="description">Task Description *</label>
        <textarea
          id="description"
          name="description"
          value={formData.description}
          onChange={handleChange}
          placeholder="Enter task description..."
          required
          rows={3}
        />
      </div>

      <div className="form-group">
        <label htmlFor="date">Task Date</label>
        <input
          type="date"
          id="date"
          name="date"
          value={formData.date}
          onChange={handleChange}
          required
        />
      </div>

      <button 
        type="submit" 
        className="submit-btn"
        disabled={isLoading || !formData.description.trim()}
      >
        {isLoading ? 'Creating...' : 'Create Task'}
      </button>
    </form>
  );
};

export default TaskForm;