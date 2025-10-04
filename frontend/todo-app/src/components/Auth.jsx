import React, { useState } from 'react';
import { 
  Authenticator, 
  Button,
  Heading,
  Flex
} from '@aws-amplify/ui-react';
import '@aws-amplify/ui-react/styles.css';
import './Theme.css';

const AuthComponent = ({ onAuthStateChange }) => {
  const [authError, setAuthError] = useState('');

  return (
    <div className="auth-container" style={{ maxWidth: 420, margin: '60px auto' }}>
      <Authenticator
        variation="modal"
        onStateChange={(state) => {
          if (state === 'signedIn') onAuthStateChange(true);
          setAuthError('');
        }}
        errorMessage={authError}
        hideSignUp={false}
      >
        {({ signOut, user }) => (
          <Flex direction="column" padding="1rem" className="panel">
            <Heading level={3} style={{ marginBottom: '0.75rem' }}>
              Welcome, {user?.attributes?.email}
            </Heading>
            <div className="divider" />
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