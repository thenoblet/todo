// Wait for the DOM to be fully loaded before running the script
document.addEventListener('DOMContentLoaded', () => {

    // --- CONFIGURATION ---
    // IMPORTANT: Replace these placeholder values with the outputs from your SAM deployment
    const COGNITO_USER_POOL_ID = 'YOUR_USER_POOL_ID';
    const COGNITO_APP_CLIENT_ID = 'YOUR_USER_POOL_CLIENT_ID';
    const API_GATEWAY_ENDPOINT = 'YOUR_API_GATEWAY_ENDPOINT';
    const AWS_REGION = 'YOUR_AWS_REGION'; // e.g., 'us-east-1'

    // --- AWS SDK & Cognito Setup ---
    AWS.config.region = AWS_REGION;
    const poolData = {
        UserPoolId: COGNITO_USER_POOL_ID,
        ClientId: COGNITO_APP_CLIENT_ID,
    };
    const userPool = new AmazonCognitoIdentity.CognitoUserPool(poolData);

    // --- DOM Elements ---
    const authContainer = document.getElementById('auth-container');
    const appContainer = document.getElementById('app-container');
    const signinForm = document.getElementById('signin-form');
    const signupForm = document.getElementById('signup-form');
    const authMessage = document.getElementById('auth-message');
    const showSignup = document.getElementById('show-signup');
    const showSignin = document.getElementById('show-signin');
    const signoutBtn = document.getElementById('signout-btn');
    const addTaskForm = document.getElementById('add-task-form');
    const taskDescriptionInput = document.getElementById('task-description');

    // --- State ---
    let idToken = null;

    // --- Event Listeners ---
    showSignup.addEventListener('click', (e) => {
        e.preventDefault();
        signinForm.classList.add('hidden');
        signupForm.classList.remove('hidden');
        authMessage.textContent = '';
    });
    showSignin.addEventListener('click', (e) => {
        e.preventDefault();
        signupForm.classList.add('hidden');
        signinForm.classList.remove('hidden');
        authMessage.textContent = '';
    });
    signupForm.addEventListener('submit', handleSignup);
    signinForm.addEventListener('submit', handleSignin);
    signoutBtn.addEventListener('click', handleSignout);
    addTaskForm.addEventListener('submit', handleAddTask);

    // --- Functions ---
    function handleSignup(event) {
        event.preventDefault();
        authMessage.textContent = '';
        const email = document.getElementById('signup-email').value;
        const password = document.getElementById('signup-password').value;

        const attributeList = [
            new AmazonCognitoIdentity.CognitoUserAttribute({ Name: 'email', Value: email })
        ];

        userPool.signUp(email, password, attributeList, null, (err, result) => {
            if (err) {
                authMessage.textContent = err.message || JSON.stringify(err);
                return;
            }
            authMessage.textContent = 'Sign up successful! Please sign in.';
            showSignin.click(); // Switch to the sign-in form
        });
    }

    function handleSignin(event) {
        event.preventDefault();
        authMessage.textContent = '';
        const email = document.getElementById('signin-email').value;
        const password = document.getElementById('signin-password').value;

        const authenticationData = { Username: email, Password: password };
        const authenticationDetails = new AmazonCognitoIdentity.AuthenticationDetails(authenticationData);
        const userData = { Username: email, Pool: userPool };
        const cognitoUser = new AmazonCognitoIdentity.CognitoUser(userData);

        cognitoUser.authenticateUser(authenticationDetails, {
            onSuccess: (session) => {
                idToken = session.getIdToken().getJwtToken();
                showApp();
                fetchTasks();
            },
            onFailure: (err) => {
                authMessage.textContent = err.message || JSON.stringify(err);
            },
        });
    }

    function handleSignout() {
        const cognitoUser = userPool.getCurrentUser();
        if (cognitoUser) {
            cognitoUser.signOut();
        }
        idToken = null;
        showAuth();
        clearTaskLists();
    }

    async function handleAddTask(event) {
        event.preventDefault();
        if (!taskDescriptionInput.value.trim()) return;

        try {
            await apiRequest('/tasks', 'POST', { description: taskDescriptionInput.value });
            taskDescriptionInput.value = ''; // Clear input field
            fetchTasks();
        } catch (error) {
            console.error('Failed to add task:', error);
            alert('Could not add task. Please try again.');
        }
    }

    async function fetchTasks() {
        try {
            const tasks = await apiRequest('/tasks', 'GET');
            renderTasks(tasks);
        } catch (error) {
            console.error('Failed to fetch tasks:', error);
            // Maybe show a user-friendly error message on the UI
        }
    }

    async function updateTaskStatus(taskId, status) {
        try {
            await apiRequest(`/tasks/${taskId}`, 'PUT', { status });
            fetchTasks();
        } catch (error) {
            console.error('Failed to update task:', error);
        }
    }

    async function deleteTask(taskId) {
        try {
            await apiRequest(`/tasks/${taskId}`, 'DELETE');
            fetchTasks();
        } catch (error) {
            console.error('Failed to delete task:', error);
        }
    }

    function renderTasks(tasks) {
        const lists = {
            Pending: document.getElementById('pending-tasks'),
            Completed: document.getElementById('completed-tasks'),
            Expired: document.getElementById('expired-tasks')
        };
        // Clear all lists before re-rendering
        clearTaskLists();

        tasks.forEach(task => {
            const li = document.createElement('li');
            li.className = 'flex items-center justify-between p-2 rounded hover:bg-gray-100';

            const descriptionSpan = document.createElement('span');
            descriptionSpan.textContent = task.Description;
            li.appendChild(descriptionSpan);

            const buttonsContainer = document.createElement('div');

            if (task.Status === 'Pending') {
                const completeBtn = createButton('Complete', 'bg-green-500', () => updateTaskStatus(task.TaskId, 'Completed'));
                buttonsContainer.appendChild(completeBtn);
                lists.Pending.appendChild(li);
            } else if (task.Status === 'Completed') {
                descriptionSpan.className = 'completed-task'; // Using custom class
                lists.Completed.appendChild(li);
            } else {
                descriptionSpan.className = 'text-red-500';
                lists.Expired.appendChild(li);
            }

            const deleteBtn = createButton('Delete', 'bg-red-500', () => deleteTask(task.TaskId));
            buttonsContainer.appendChild(deleteBtn);

            li.appendChild(buttonsContainer);
        });
    }

    function createButton(text, classes, onClick) {
        const button = document.createElement('button');
        button.textContent = text;
        button.className = `text-white px-2 py-1 rounded text-sm ml-2 ${classes}`;
        button.onclick = onClick;
        return button;
    }

    async function apiRequest(path, method, body) {
        if (!idToken) {
            throw new Error("User is not authenticated.");
        }

        const headers = {
            'Content-Type': 'application/json',
            'Authorization': idToken,
        };

        const options = { method, headers };
        if (body) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(API_GATEWAY_ENDPOINT + path, options);
        if (response.status === 204) return; // No Content
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`API request failed: ${errorData.message || response.statusText}`);
        }
        return response.json();
    }

    // --- UI Helper Functions ---
    function showApp() {
        authContainer.classList.add('hidden');
        appContainer.classList.remove('hidden');
    }

    function showAuth() {
        appContainer.classList.add('hidden');
        authContainer.classList.remove('hidden');
        signinForm.classList.remove('hidden'); // Default to signin view
        signupForm.classList.add('hidden');
    }

    function clearTaskLists() {
        document.getElementById('pending-tasks').innerHTML = '';
        document.getElementById('completed-tasks').innerHTML = '';
        document.getElementById('expired-tasks').innerHTML = '';
    }

    // --- Initial Check ---
    // Check if a user is already signed in on page load
    const cognitoUser = userPool.getCurrentUser();
    if (cognitoUser) {
        cognitoUser.getSession((err, session) => {
            if (err) {
                console.log('Session error:', err);
                showAuth();
                return;
            }
            if (session.isValid()) {
                idToken = session.getIdToken().getJwtToken();
                showApp();
                fetchTasks();
            } else {
                showAuth();
            }
        });
    } else {
        showAuth();
    }
});
