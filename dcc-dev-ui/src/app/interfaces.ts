export interface PullRequest {
  avatarUrl: String;
  branch: String;
  description: String;
  head: String;
  number: Number;
  title: String;
  url: String;
  user: String;
}

export interface Candidate {
  artifact: String;
  build: {
    commitId: String,
    number: Number,
    prNumber: Number,
    timestamp: Number,
    url: String
  };
  pr: PullRequest;
};

export interface Portal {
  autoDeploy: Boolean,
  autoRefresh: Boolean,
  autoRemove: Boolean,
  description: String,
  id: String,
  properties: any,
  state: String, // maybe enum?
  systemProperties: {
    'management.port': String,
    'server.port': String
  },
  target: Candidate,
  ticket: any,
  url: String
};