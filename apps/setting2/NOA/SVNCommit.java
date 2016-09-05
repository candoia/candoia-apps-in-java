package setting2.NOA;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Created by nmtiwari on 7/25/16.
 */


/**
 * Concrete implementation of a commit for SVN.
 *
 * @author rdyer
 */
public class SVNCommit {
    // since SVN uses longs, mirror the commit ID here as a long to avoid boxing/unboxing to String
    private long svnId = -1;
    private VCSModule conn;
    protected static final ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
    private Map<String, String> changedPaths = new HashMap<String, String>();
	private Map<String, String> addedPaths = new HashMap<String, String>();
	private Map<String, String> removedPaths = new HashMap<String, String>();
	protected String message;
	protected Date date;
	protected String committer;
	private SVNLogEntry entry;

    /** {@inheritDoc} */
    public void setId(final String id) {
        this.svnId = Long.parseLong(id);
    }

    // the repository the commit lives in - should already be connected!
    private final SVNRepository repository;

    public SVNCommit(final SVNRepository repository, VCSModule conn, SVNLogEntry entry) {
        this.conn =  conn;
        this.repository = repository;
        this.entry = entry;
    }

    /** {@inheritDoc} */
    protected String getFileContents(final String path) {
        try {
            buffer.reset();
            repository.getFile(path, svnId, null, buffer);
            return buffer.toString();
        } catch (final SVNException e) {
            e.printStackTrace();
            return "";
        }
    }
    
	public void setChangedPaths(final Map<String, String> changedPaths) {
		this.changedPaths = changedPaths;
	}

	public void setRemovedPaths(final Map<String, String> removedPaths) {
		this.removedPaths = removedPaths;
	}
	
	public void setAddedPaths(final Map<String, String> addedPaths) {
		this.addedPaths = addedPaths;
	}
	
	public void setCommitter(final String committer) {
		this.committer = committer;
	}
	
	public void setDate(final Date date) {
		this.date = date;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public long getId(){
		return this.svnId;
	}
	
	public ArrayList<String> getFiles(){
		ArrayList<String> files = new ArrayList<>();
		 for (final Iterator changedPaths = this.entry.getChangedPaths().keySet().iterator(); changedPaths.hasNext(); ) {
			 final SVNLogEntryPath entryPath = (SVNLogEntryPath) this.entry.getChangedPaths().get(changedPaths.next());
			 try {
				if (repository.checkPath(entryPath.getPath(), this.entry.getRevision()) == SVNNodeKind.FILE) {
					files.add(entryPath.getPath());
				 }
			} catch (SVNException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
		 return files;
	}
}
