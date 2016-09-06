package setting5.fileAssocaitionMining;

public class SVNTicket {
	String id;
	String message;
	String summary;
	String title;
	String description;
	String status;
	
	public SVNTicket(String id) {
		this.id = id;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setSummary(String summary) {
		this.summary = summary;	
	}
	
	public void setStatus(String status) {
		this.status = status;	
	}

	public String getId(){
		return this.id;
	}
}
