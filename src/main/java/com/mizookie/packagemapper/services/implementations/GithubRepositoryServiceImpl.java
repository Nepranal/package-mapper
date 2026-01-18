package com.mizookie.packagemapper.services.implementations;

import com.mizookie.packagemapper.services.GithubRepositoryService;
import com.mizookie.packagemapper.utils.FileService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the GithubRepositoryService interface that provides methods
 * for downloading public and private repositories from GitHub.
 */
@Slf4j
@Service
@Component
public class GithubRepositoryServiceImpl implements GithubRepositoryService {

    private final FileRepositoryBuilder builder = new FileRepositoryBuilder();
    @Value("${repository.directory}")
    private String localRepositoryDirectory;
    // Absolute path to the user's repository directory (local)
    private String userRepositoryDirectory;
    private Git git;

    /**
     * Downloads a public GitHub repository to the local file system.
     *
     * @param repositoryUrlString The URL of the repository to download.
     * @return A message indicating the result of the download operation.
     */
    @Override
    public String downloadPublicRepository(String repositoryUrlString) throws GitAPIException {
        try {
            // Extract the repository name from the URL
            String repositoryName = getRepositoryName(repositoryUrlString);
            userRepositoryDirectory = localRepositoryDirectory + "/" + repositoryName;
            File localDirectory = new File(userRepositoryDirectory);

            // Close the previous Git instance if it exists
            if (git != null) {
                git.close();
            }

            // Clone the repository to the local directory
            git = Git.cloneRepository()
                    .setURI(repositoryUrlString) // Set the repository URL
                    .setDirectory(localDirectory) // Set the local directory
                    .call();

            return "Repository downloaded successfully!";
        } catch (GitAPIException e) {
            return "Failed to clone repository: " + e.getMessage();
        }
    }

    /**
     * Downloads a private GitHub repository to the local file system.
     *
     * @param repositoryUrlString The URL of the repository to download.
     * @param token               The access token for the private repository.
     */
    @Override
    public void downloadPrivateRepository(String repositoryUrlString, String token) {
        // TODO: Implement method to download private repositories
    }

    /**
     * Deletes a GitHub repository from the local file system.
     *
     * @return A message indicating the result of the delete operation.
     */
    @Override
    public String deleteRepository() throws IOException, GitAPIException {
        Path directoryPath = null;
        // Force delete all repositories if invoked right after the application starts
        if (userRepositoryDirectory == null) {
            directoryPath = Paths.get(localRepositoryDirectory);
            FileService.removeRecursively(directoryPath.toFile());
            Files.createDirectories(directoryPath);
            return "All repositories have been deleted.";
        }
        // Delete specific repository directory if it exists
        directoryPath = Paths.get(userRepositoryDirectory);
        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            // Close the Git repository and shutdown the Git instance
            try {
                if (git != null) {
                    git.close(); // Close the Git instance
                    git = null; // Set git to null after closing
                }
                // Delete the repository directory and its contents recursively
                log.info("Deleting repository directory: {}", directoryPath);
                FileService.removeRecursively(directoryPath.toFile());
            } catch (Exception e) {
                log.error("Failed to delete repository directory: {}", e.getMessage());
                throw e;
            }
            userRepositoryDirectory = null;
            return "Repository directory deleted!";
        } else {
            return "Repository directory not found!";
        }
    }

    public void fetchAll(String repositoryName) throws IOException, GitAPIException {
        Git git = new Git(builder.setGitDir(new File(new File(localRepositoryDirectory, repositoryName), ".git"))
                .readEnvironment()
                .findGitDir()
                .build());
        git.remoteList().call().forEach(remote -> {
            try {
                git.fetch().setRemote(remote.getName()).setRefSpecs(remote.getFetchRefSpecs()).call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * This function allows you to get commit versions of a repository. By default, since this is also how git behaves,
     * the function gives the commit versions viewable by the current commit version. If {@code version} is specified, then it will show
     * the commit versions viewable from that version.
     *
     * @param repositoryName name of the repository.
     * @param version        repository commit version. Specifying this has the side effect
     *                       of changing the repository's commit version
     * @param limit          Limit the number of commit versions to fetch
     * @param all            Same as git log's {@code --all} parameter
     * @return List of commit versions
     */
    public List<String> getRepoCommitVersions(String repositoryName, String version, int limit, boolean all) throws GitAPIException, IOException {
        Git git = new Git(builder.setGitDir(new File(new File(localRepositoryDirectory, repositoryName), ".git"))
                .readEnvironment()
                .findGitDir()
                .build());

        if (version != null) {
            git.checkout().setName(version).call();
        }

        ArrayList<String> commits = new ArrayList<>();
        LogCommand cmd = git.log().setMaxCount(limit);
        if (all) {
            cmd = cmd.all();
        }
        cmd.call().iterator().forEachRemaining(revCommit -> commits.add(revCommit.getName()));
        return commits;
    }

    public List<String> getLogAll(String repositoryName) throws GitAPIException, IOException {
        return getRepoCommitVersions(repositoryName, null, Integer.MAX_VALUE, true);
    }

    public List<String> getRepoCommitVersions(String repositoryName, String version, int limit) throws GitAPIException, IOException {
        return getRepoCommitVersions(repositoryName, version, limit, false);
    }

    // commit id based on the current HEAD.
    public String getCurrentCommit(String repositoryName) throws GitAPIException, IOException {
        return getRepoCommitVersions(repositoryName, null, 1).get(0);
    }

    public void checkoutCommit(String repositoryName, String version) throws IOException, GitAPIException {
        getRepoCommitVersions(repositoryName, version, 0);
    }

    // Helper method to extract the repository name from the URL
    private String getRepositoryName(String repositoryUrlString) {
        return FileService.getFileNameWithoutExtension(repositoryUrlString);
    }

    public List<String> getAllRepo() {
        return FileService.getCurrentDirectory(localRepositoryDirectory);
    }
}