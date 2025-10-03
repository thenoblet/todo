import React, { useState } from 'react';
import { Auth } from 'aws-amplify';
import { 
  Authenticator, 
  Button,
  Heading,
  Text,
  Flex
} from '@aws-amplify/ui-react';
import '@aws-amplify/ui-react/styles.css';

const AuthComponent = ({ onAuthStateChange }) => {
  const [authError, setAuthError] = useState('');

  return (
    <div className="auth-container" style={{ width: '400px', margin: '50px auto' }}>
      <Authenticator
        variation="modal"
        onStateChange={(state) => {
          console.log('Auth state changed:', state);
          if (state === 'signedIn') {
            onAuthStateChange(true);
          }
          setAuthError('');
        }}
        errorMessage={authError}
        hideSignUp={false}
      >
        {({ signOut, user }) => (
          <Flex direction="column" padding="1rem">
            <Heading level={3}>Welcome, {user.attributes?.email}</Heading>
            <Button onClick={signOut} variation="primary">
              Sign Out
            </Button>
          </Flex>
        )}
      </Authenticator>
    </div>
  );
};

export default AuthComponent;