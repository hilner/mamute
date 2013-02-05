package br.com.caelum.brutal.model;

import static br.com.caelum.brutal.infra.NormalizerBrutal.toSlug;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

@Entity
public class QuestionInformation {

	@Id
	@GeneratedValue
	private Long id;

	@Type(type = "text")
	@Length(min = 15)
	@NotEmpty
	private String title;

	@Type(type = "text")
	@Length(min = 30)
	@NotEmpty
	private String description;

	@Type(type = "text")
	@NotEmpty
	private String sluggedTitle;

	@ManyToOne(optional = false)
	private final User author;

	@Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
	private final DateTime createdAt = new DateTime();

	@Embedded
	private Moderation moderation;

	@ManyToMany
	private final List<Tag> tags = new ArrayList<>();
	@Lob
	private String markedDescription;

	private UpdateStatus status;

	public QuestionInformation(String title, String description, User author) {
		this.author = author;
		setTitle(title);
		setDescription(description);
	}

	public void moderate(User moderator, UpdateStatus status) {
		if (this.moderation != null) {
			throw new IllegalStateException("Already moderated");
		}
		this.status = status;
		this.moderation = new Moderation(moderator);
	}

	private void setTitle(String title) {
		this.title = title;
		this.sluggedTitle = toSlug(title);
	}

	private void setDescription(String description) {
		this.description = description;
		this.markedDescription = MarkDown.parse(description);
	}

	public void addTag(Tag tag) {
		this.tags.add(tag);
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getSluggedTitle() {
		return sluggedTitle;
	}

	public String getMarkedDescription() {
		return markedDescription;
	}

	public String getTagsAsString() {
		StringBuilder sb = new StringBuilder();
		for (Tag t : tags) {
			sb.append(t.getName());
			sb.append(" ");
		}
		return sb.toString();
	}

	public User getAuthor() {
		return author;
	}

}