package jiracommitviewer.action;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jiracommitviewer.GitManager;
import jiracommitviewer.MultipleGitRepositoryManager;

import org.apache.commons.lang.StringUtils;

/**
 * Manage 1 or more repositories
 */
@SuppressWarnings("serial")
public class ViewGitRepositoriesAction extends GitActionSupport {

    public ViewGitRepositoriesAction(MultipleGitRepositoryManager manager) {
        super (manager);
    }

    public Collection<GitManager> getRepositories() {
        List<GitManager> subversionManagers = new ArrayList<GitManager>(getMultipleRepoManager().getRepositoryList());

        Collections.sort(
                subversionManagers,
                new Comparator<GitManager>() {
                    public int compare(GitManager left, GitManager right) {
                        return StringUtils.defaultString(left.getDisplayName()).compareTo(
                                StringUtils.defaultString(right.getDisplayName())
                        );
                    }
                }
        );

        return subversionManagers;
    }
}
