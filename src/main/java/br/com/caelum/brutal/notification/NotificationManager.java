package br.com.caelum.brutal.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import br.com.caelum.brutal.dao.WatcherDAO;
import br.com.caelum.brutal.infra.ThreadPoolContainer;
import br.com.caelum.brutal.mail.action.EmailAction;
import br.com.caelum.brutal.model.Question;
import br.com.caelum.brutal.model.watch.Watcher;
import br.com.caelum.vraptor.ioc.Component;

@Component
public class NotificationManager {

	private final WatcherDAO watchers;
	private final NotificationMailer mailer;
	private final ThreadPoolContainer threadPoolContainer;
	private final static Logger LOG = Logger.getLogger(NotificationManager.class); 

	public NotificationManager(WatcherDAO watchers, NotificationMailer notificationMailer, ThreadPoolContainer threadPoolContainer) {
		this.watchers = watchers;
		this.mailer = notificationMailer;
		this.threadPoolContainer = threadPoolContainer;
	}
	
	public void sendEmailsAndInactivate(EmailAction emailAction) {
		Question question = emailAction.getQuestion();
		List<Watcher> watchList = watchers.of(question);
		
		List<NotificationMail> mails = buildMails(emailAction, watchList);
		sendMailsAsynchronously(mails);
	}

	private List<NotificationMail> buildMails(EmailAction emailAction, List<Watcher> watchList) {
		List<NotificationMail> mails = new ArrayList<NotificationMail>();
		for (Watcher watcher : watchList) {
			boolean sameAuthor = watcher.getWatcher().getId().equals(emailAction.getWhat().getAuthor().getId());
			if (!sameAuthor) {
				mails.add(new NotificationMail(emailAction, watcher.getWatcher()));
				watcher.inactivate();
			}
		}
		return mails;
	}

	private void sendMailsAsynchronously(final List<NotificationMail> mails) {
		threadPoolContainer.execute(new Runnable() {
			@Override
			public void run() {
				for (NotificationMail notificationMail : mails) {
					try {
						LOG.info("Sending email: " + notificationMail);
						mailer.send(notificationMail);
					} catch (Exception e) {
						LOG.error("Could not send email: " + notificationMail, e);
					}
				}
			}
		});
	}

}
