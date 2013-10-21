package jiracommitviewer.index;

import java.io.IOException;
import java.util.BitSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

@SuppressWarnings("serial")
public class ProjectRevisionFilter extends AbstractRevisionFilter {
	
    private final String projectKey;

    public ProjectRevisionFilter(final IssueManager issueManager, final PermissionManager permissionManager, final User user, 
    		final String projectKey) {
        super(issueManager, permissionManager, user);
        this.projectKey = projectKey;
    }

    @SuppressWarnings("deprecation")
	@Override
    public DocIdSet getDocIdSet(final IndexReader indexReader) throws IOException {
        final BitSet bitSet = new BitSet(indexReader.maxDoc());

        final TermDocs termDocs = indexReader.termDocs(new Term(CommitIndexer.FIELD_PROJECTKEY, projectKey));
        while (termDocs.next()) {
            final int docId = termDocs.doc();
            final Document theDoc = indexReader.document(docId, issueKeysFieldSelector);

            boolean allow = false;
            final String[] issueKeys = theDoc.getValues(CommitIndexer.FIELD_ISSUEKEY);

            if (issueKeys != null) {
                for (final String issueKey : issueKeys) {
                    final Issue anIssue = issueManager.getIssueObject(StringUtils.upperCase(issueKey));
                    if (anIssue != null && permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, anIssue, user)) {
                        allow = true;
                        break;
                    }
                }
            }

            bitSet.set(docId, allow);
        }

        return new DocIdBitSet(bitSet);
    }
}
