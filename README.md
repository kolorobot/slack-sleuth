# Channel Sleuth

The little app that helps you uncover insights and trends within your Slack conversations in public channels.

## Prerequisites

- Create a Slack App
  - Go to https://api.slack.com/apps
  - Create new app from scratch in a desired workspace
  - Once app is created, go to `OAuth & Permissions` and add the following `Bot Token Scopes` :
    - `channels:history`
    - `channels:read`
    - `usergroups:read`
    - `users:read`
  - Now, install the application in workspace
  - Copy `Bot User OAuth Token`
  - Add the app to the channel you want to analyze
  - To view and/or change the app settings go to https://api.slack.com/apps

## Running the app with Gradle

`./gradlew run --args='-t <SLACK_BOT_TOKEN> <COMMAND> <ARGS>'`

Commands:
- channels
- history
- analyzer
- user

### Get channel list

`./gradlew run --args='-t <SLACK_BOT_TOKEN> channels'`

### Get conversation history

`./gradlew run --args='-t <SLACK_BOT_TOKEN> history -c <CHANNEL_ID> -o <FILE_PATH>'`

### Analyze history

`./gradlew run --args='-t <SLACK_BOT_TOKEN> history -c <CHANNEL_ID> -o <FILE_PATH>'`

## Assembly the app

- Run `./gradlew clean assemble`
- Navigate to `build/distributions` and locate the `zip` file containing the assembled app
