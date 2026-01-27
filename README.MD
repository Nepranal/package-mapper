# Package Mapper

Package Mapper is an open-source tool that provides a visual representation of the classes and their dependencies within a GitHub repository.

This version of the project currently does not handle any specific import style of programming languages. Instead, it just [naively figure out whether there any mentions of some other files](#resolver). It should be quite straightforward to add a new way to handle the language of interest (well, not considering how complicated the import system is for that language).

# Additions & Caveats

This project is just a version of the [original package mapper](https://github.com/MarzukhAsjad/package-mapper-archive). It provides a visualising system but it doesn't necessarily work out the actual dependencies. Right now, I have only worked it out so that so long as the file name's (just the name, the full path is dropped) mentioned in another file, then that connection will be made

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

# Design

The main ideas of the mapper is split into 2 kinds:

1. How would you like to visit the files
2. How would you like to figure out the dependencies

You only need to specify how to do these two things which will be described in the following sections

# Specifying Which Files

Check out `visitors` under the backend folders. You will find a simple implementation of `FileVisitor` called the `VisibleFileVisitor`. These two just follows the `SimpleFileVisitor` class use to walk through folders. You can find out more [from the official java tutorial](https://docs.oracle.com/javase/tutorial/essential/io/walk.html). To make it simple, just extend from `FileVisitor`.

Once you wrote one, you can specify it under `utils/FileService.java` under `getFiles()`

```java
// Visit files in specified directory
public List<String> getFiles(String directoryPath) {
    FileVisitor visitor = new VisibleFileVisitor(); // This one
    //...
}
```

# Resolver

This is the main bulk of the mapper. The task is to write a resolver. I didn't specify any specific way you can do this, but any resolver should just create a function that takes in the file path and read the file themselves.

The start of this component falls under `services/implementations/AnalyserServiceImpl.java` under the `analyse(String repositoryPath, String version)` method:

```java
@Override
public void analyse(String repositoryPath, String version) throws IOException, GitAPIException, InterruptedException {
    //...
}
```

What you'll find is that this method is a multithreaded one. It works by basically giving out the task and then just wait until everyone's finished. The task will all get the file themselves. The class that handles the multithreaded task is `AnalyzerTask`. You'll find it if you scroll a bit down within the same file.

Within `AnalyzerTask`'s `run()`, you'll find this block of code:

```java
switch (FileService.getFileExtension(filePath)) {
    case ".py":
    case ".java":
    default:
        results = naiveResolver.solve(filePaths, filePath); // This method is thread-safe
}
```

This is where you can write your own resolver. As you can see, it currently only has `NaiveResolver` under `resolver` which is slow and probably cannot figure out the actual import statement. The actual implementation is not constrained but at this point, you can really only access the `filePath` which gives a path to a file and `filePaths` which is all files in the repo directory

# Coming Soon Features

Please checkout [package-mapper-archive](https://github.com/MarzukhAsjad/package-mapper-archive) for more info on this
