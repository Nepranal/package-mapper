# Package Mapper

Package Mapper is an open-source tool that provides a visual representation of the classes and their dependencies within a GitHub repository. It supports any JavaScript and Java based projects and aims to expand to other languages in the future.

# Additions & Caveats

This one is obviously just a version of the [original package mapper](https://github.com/MarzukhAsjad/package-mapper-archive). It provides a visualising system but it doesn't necessarily work out the actual dependencies. Right now, I have only worked it out so that so long as the file name's (just the name, the full path is dropped) mentioned in another file, then that connection will be made. Something more specific is still in the works

This version also added a visualiser. Have a look at `package-mapper-viewer`. It contains a react (mostly) implementation of [Mobile patent suits](https://observablehq.com/@d3/mobile-patent-suits) from the d3 library

# Installation

To use the Package Mapper, just clone the repo and run it with whatever code editor you got

# Backend Usage

Before getting into it, please have a `.env` file and just place it in the same directory this file is located.

```Dotenv
REPOSITORY_DIRECTORY=<absolute path where all repositories will be downloaded>
ANALYSIS_DIRECTORY=<absolute path where all the analysis logs will be stored>
LOGS_DIRECTORY=<absolute path where all general logs will be stored>
ANALYSIS_THREADS=<number of threads for file analysis>
```

The backend has a collection of endpoints. Some are from the previous version of the mapper so here's what I have used (and what the frontend is going to use):

## `/repository/download`

`POST`

```json
{
  "url": "https://github.com/Nepranal/chained-reconstructions.git"
}
```

Please notice the `.git`.

## `/repository/branches?repo=&version=`

`GET`

This has the request parameter `repo` and an optional `version`. Use this to specify commit versions of the repository you want from a particular `version`.

## `/repository/log?repo=`

`GET`

Similar to `/repository/branches?repo=&version=` but get all reachable commit versions

## `/repository/fetch?repo=`

`PUT`

perform `git fetch --all` on `repo`.

## `/repository/all`

`GET`

Get all cloned repository

## `/analyse/graph?repo=&version=`

`GET`

This is the main thing. Perform dependency analysis based on `repo` and `version` (not optional). This returns a dependency graph that looks like this

```json
[
  {
    "source": "GameClient.py",
    "target": "README.md",
    "type": "import"
  },
  {
    "source": "GameServer.py",
    "target": "README.md",
    "type": "import"
  },
  {
    "source": "UserInfo.txt",
    "target": "README.md",
    "type": "import"
  },
  {
    "source": "UserInfo.txt",
    "target": "GameServer.py",
    "type": "import"
  },
  {
    "source": "GameServer.py",
    "target": "GameClient.py",
    "type": "import"
  }
]
```

# Visualiser

Before using the visualiser, please run the backend first. The visualiser has simple components to it:

![alt text](<imgs/Screenshot 2026-01-25 at 12.52.17 PM.png>)

You'll need to first specify the repo's name in `Repository name` box and choose the `Commit version`. Once a version is picked, the visualiser will try to fetch the graph and shows it.

If you want to download a new repo, use `Repository Url`.

You can move the graph around and it will also highlight import and export using colours

![alt text](<imgs/Jan-25-2026 13-05-35.gif>)

The colour of the vertex depends on the file's parent path. This way if two files have the same name, they can still be differentiated by their vertex's colour.

To run it, just run it with

```
cd package-mapper-viewer
npm install
npm run dev
```

# Coming Soon Features

Please checkout [package-mapper-archive](https://github.com/MarzukhAsjad/package-mapper-archive) for more info on this
