import { Amplify } from 'aws-amplify';

const awsConfig = {
  Auth: {
    region: 'eu-central-1',
    userPoolId: 'eu-central-1_tcKfLPGWa',
    userPoolWebClientId: 'md3ialdpjs7d5rlt6hu2oa5tt',
    authenticationFlowType: 'USER_SRP_AUTH',
  },
  API: {
    endpoints: [
      {
        name: 'todoApi',
        endpoint: 'https://90145i3bc7.execute-api.eu-central-1.amazonaws.com/prod',
        region: 'eu-central-1' 
      }
    ]
  }
};

try {
  Amplify.configure(awsConfig);
} catch (error) {
  console.error('Error configuring Amplify:', error);
}

export default awsConfig;