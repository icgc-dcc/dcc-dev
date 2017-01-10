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

interface JiraAvatarUrls {
  '16x16': string;
  '24x24': string;
  '32x32': string;
  '48x48': string;
}

interface JiraAuthor {
  active: boolean;
  JiraavatarUrls: JiraAvatarUrls;
  displayName: string;
  emailAddress: string;
  key: string;
  name: string;
  self: string;
  timeZone: string;
}

interface JiraUpdateAuthor {
  active: boolean;
  JiraavatarUrls: JiraAvatarUrls;
  displayName: string;
  emailAddress: string;
  key: string;
  name: string;
  self: string;
  timeZone: string;
}

export interface JiraComment {
  author: JiraAuthor;
  body: string;
  created: Date;
  id: string;
  self: string;
  JiraupdateAuthor: JiraUpdateAuthor;
  updated: Date;
}
