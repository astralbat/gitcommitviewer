package jiracommitviewer.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.AbstractRepository;

import org.apache.commons.lang.StringUtils;

/**
 * View 1 or more repositories
 */
@SuppressWarnings("serial")
public class ViewGitRepositoriesAction extends GitActionSupport {

    public ViewGitRepositoriesAction(final RepositoryManager manager) {
        super (manager);
    }

    /**
     * Gets a list of repositories for display.
     * 
     * @return the list of repositories. Never {@code null}
     */
    public Collection<AbstractRepository> getRepositories() {
        final List<AbstractRepository> repositories = new ArrayList<AbstractRepository>(repositoryManager.getRepositoryList());

        Collections.sort(repositories,
            new Comparator<AbstractRepository>() {
                public int compare(final AbstractRepository left, final AbstractRepository right) {
                    return StringUtils.defaultString(left.getDisplayName()).compareTo(
                            StringUtils.defaultString(right.getDisplayName())
                    );
                }
            }
        );

        return repositories;
    }
}
