package org.carlspring.strongbox.storage.resolvers;

import org.apache.maven.artifact.Artifact;
import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.annotations.ArtifactExistenceState;
import org.carlspring.strongbox.annotations.ArtifactResource;
import org.carlspring.strongbox.annotations.ArtifactResourceMapper;
import org.carlspring.strongbox.io.RandomInputStream;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author mtodorov
 */
@Component
public class InMemoryLocationResolver
        extends AbstractLocationResolver
{

    private static final Logger logger = LoggerFactory.getLogger(InMemoryLocationResolver.class);

    private String alias = "in-memory";


    public InMemoryLocationResolver()
    {
    }

    @Override
    public InputStream getInputStream(String repositoryId,
                                      String artifactPath)
            throws IOException
    {
        for (Map.Entry entry : getConfiguration().getStorages().entrySet())
        {
            Storage storage = (Storage) entry.getValue();

            if (storage.containsRepository(repositoryId))
            {
                logger.debug("Checking in storage " + storage.getBasedir() + "...");

                final Map<String, Repository> repositories = storage.getRepositories();

                for (Map.Entry<String, Repository> e : repositories.entrySet())
                {
                    Repository r = e.getValue();

                    logger.debug("Checking in repository " + r.getId() + "...");

                    Artifact artifact = ArtifactUtils.convertPathToArtifact(artifactPath);

                    final ArtifactResource resource = ArtifactResourceMapper.getResource(artifact.getGroupId(),
                                                                                         artifact.getArtifactId(),
                                                                                         artifact.getVersion());

                    if (resource == null)
                    {
                        logger.debug("Artifact " + artifact.toString() + " not found.");

                        return null;
                    }
                    else
                    {
                        // Create random data.
                        logger.debug("Generating stream with " + resource.length() + " bytes.");

                        return new RandomInputStream(resource.length());
                    }
                }
            }
        }

        return null;
    }

    @Override
    public OutputStream getOutputStream(String repositoryId,
                                        String artifactPath)
            throws IOException
    {
        for (Map.Entry entry : getConfiguration().getStorages().entrySet())
        {
            Storage storage = (Storage) entry.getValue();

            if (storage.containsRepository(repositoryId))
            {
                if (!artifactPath.contains("/maven-metadata."))
                {
                    Artifact artifact = ArtifactUtils.convertPathToArtifact(artifactPath);
                    ArtifactResourceMapper.addResource(ArtifactResourceMapper.getArtifactResourceInstance(repositoryId,
                                                                                                          artifact,
                                                                                                          10000L, // Hard-coding to 10 KB as we can't guess
                                                                                                                  // the size at this point and we shouldn't be
                                                                                                                  // caring about this too much as it's in memory
                                                                                                          ArtifactExistenceState.EXISTS));
                }

                return new ByteArrayOutputStream();
            }
        }

        return null;
    }

    @Override
    public void delete(String repositoryId,
                       String path,
                       boolean force)
            throws IOException
    {
        for (Map.Entry entry : getConfiguration().getStorages().entrySet())
        {
            Storage storage = (Storage) entry.getValue();

            if (storage.containsRepository(repositoryId))
            {
                if (!path.contains("/maven-metadata."))
                {
                    Artifact artifact = ArtifactUtils.convertPathToArtifact(path);
                    ArtifactResourceMapper.removeResources(artifact.getGroupId(),
                                                           artifact.getArtifactId(),
                                                           artifact.getVersion());

                    logger.debug("Removed /" + repositoryId + path);
                }
            }
        }
    }

    @Override
    public void deleteTrash(String repositoryId)
            throws IOException
    {
        logger.debug("Emptying trash for repositoryId " + repositoryId + "...");

        // Not much to implement (at least for the time-being)
    }

    @Override
    public void deleteTrash()
            throws IOException
    {
        logger.debug("Emptying trash for all repositories...");

        // Not much to implement (at least for the time-being)
    }

    @Override
    public void initialize()
    {
        logger.debug("Initialized InMemoryLocationResolver.");
    }

    @Override
    public String getAlias()
    {
        return alias;
    }

    @Override
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

}
