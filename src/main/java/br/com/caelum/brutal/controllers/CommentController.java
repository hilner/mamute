package br.com.caelum.brutal.controllers;

import br.com.caelum.brutal.auth.rules.PermissionRulesConstants;
import br.com.caelum.brutal.brutauth.auth.rules.ModeratorOrKarmaRule;
import br.com.caelum.brutal.dao.CommentDAO;
import br.com.caelum.brutal.dao.WatcherDAO;
import br.com.caelum.brutal.infra.ModelUrlMapping;
import br.com.caelum.brutal.infra.NotFoundException;
import br.com.caelum.brutal.mail.action.EmailAction;
import br.com.caelum.brutal.model.Comment;
import br.com.caelum.brutal.model.LoggedUser;
import br.com.caelum.brutal.model.User;
import br.com.caelum.brutal.model.interfaces.Watchable;
import br.com.caelum.brutal.model.watch.Watcher;
import br.com.caelum.brutal.notification.NotificationManager;
import br.com.caelum.brutal.validators.CommentValidator;
import br.com.caelum.brutauth.auth.annotations.AccessLevel;
import br.com.caelum.brutauth.auth.annotations.SimpleBrutauthRules;
import br.com.caelum.vraptor4.Controller;
import br.com.caelum.vraptor4.Post;
import br.com.caelum.vraptor4.Result;

@Controller
public class CommentController {

	private final Result result;
	private final CommentDAO comments;
	private final CommentValidator validator;
	private final ModelUrlMapping urlMapping;
	private final LoggedUser currentUser;
	private final NotificationManager notificationManager;
	private final WatcherDAO watchers;

	public CommentController(Result result, LoggedUser currentUser, CommentDAO comments,
			CommentValidator validator, ModelUrlMapping urlMapping,
			NotificationManager notificationManager, WatcherDAO watchers) {
		this.result = result;
		this.currentUser = currentUser;
		this.comments = comments;
		this.validator = validator;
		this.urlMapping = urlMapping;
		this.notificationManager = notificationManager;
		this.watchers = watchers;
	}

	@SimpleBrutauthRules({ModeratorOrKarmaRule.class})
	@AccessLevel(PermissionRulesConstants.CREATE_COMMENT)
	@Post("/{onWhat}/{id}/comentar")
	public void comment(Long id, String onWhat, String comment, boolean watching) {
		User current = currentUser.getCurrent();
		Comment newComment = new Comment(current, comment);
		Class<?> type = getType(onWhat);
		
		validator.validate(newComment);
		validator.onErrorUse(http()).setStatusCode(400);
		
		br.com.caelum.brutal.model.Post commentable = comments.loadCommentable(type, id);
		commentable.add(newComment);
		comments.save(newComment);
		Watchable watchable = commentable.getMainThread();
		notificationManager.sendEmailsAndInactivate(new EmailAction(newComment, commentable));
		if (watching) {
			watchers.add(watchable, new Watcher(current));
		} else {
			watchers.removeIfWatching(watchable, new Watcher(current));
		}
    	
    	result.forwardTo(BrutalTemplatesController.class).comment(newComment);
	}

	@Post("/comentario/editar/{id}")
	public void edit(Long id, String comment) {
		Comment original = comments.getById(id);
		if (!currentUser.getCurrent().isAuthorOf(original)) {
			result.use(status()).badRequest("comment.edit.not_author");
			return;
		}
		if (validator.validate(comment)) {
			original.setComment(comment);
			comments.save(original);
			result.forwardTo(BrutalTemplatesController.class).comment(original);
		}
		validator.onErrorUse(http()).setStatusCode(400);
	}
	
	private Class<?> getType(String name) {
		try {
			return urlMapping.getClassFor(name);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException(e);
		}
	}
}
