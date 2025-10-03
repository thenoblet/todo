import { Auth } from 'aws-amplify';
import axios from 'axios';

class TodoApi {
  constructor() {
    this.apiUrl = 'https://90145i3bc7.execute-api.eu-central-1.amazonaws.com/prod';
  }

  async getAuthToken() {
    try {
      const session = await Auth.currentSession();
      const token = session.getIdToken().getJwtToken(); 
      console.log('Auth token retrieved successfully');
      return token;
    } catch (error) {
      console.error('Error getting auth token:', error);
      throw error;
    }
  }

  async makeRequest(method, endpoint, data = null) {
    try {
      const token = await this.getAuthToken();
      const config = {
        method,
        url: `${this.apiUrl}${endpoint}`,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      };

      if (data) {
        config.data = data;
      }

      const response = await axios(config);
      return response.data;
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  async getTasks() {
    return this.makeRequest('get', '/tasks');
  }

  async createTask(taskData) {
    const { description, date } = taskData || {};
    const deadline = date ? new Date(date).getTime() : 0;

    const payload = {
      title: description || '',
      description: description || '',
      deadline,
    };

    return this.makeRequest('post', '/tasks', payload);
  }

  async updateTask(taskId, taskData) {
    return this.makeRequest('put', `/tasks/${taskId}`, taskData);
  }

  async deleteTask(taskId) {
    return this.makeRequest('delete', `/tasks/${taskId}`);
  }
}

export default new TodoApi();