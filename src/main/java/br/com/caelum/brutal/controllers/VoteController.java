package br.com.caelum.brutal.controllers;

import br.com.caelum.brutal.auth.Logged;
import br.com.caelum.brutal.dao.VoteDAO;
import br.com.caelum.brutal.model.Answer;
import br.com.caelum.brutal.model.Question;
import br.com.caelum.brutal.model.User;
import br.com.caelum.brutal.model.Votable;
import br.com.caelum.brutal.model.Vote;
import br.com.caelum.brutal.model.VoteType;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Resource;
import br.com.caelum.vraptor.Result;

@Resource
public class VoteController {

	private final Result result;
	private final User currentUser;
	private final VoteDAO votes;

	public VoteController(Result result, User currentUser, VoteDAO voteDAO) {
		this.result = result;
		this.currentUser = currentUser;
		this.votes = voteDAO;
	}

	@Logged
	@Post("/question/{id}/up")
	public void voteQuestionUp(Long id) {
		tryToVoteQuestion(id, VoteType.UP, Question.class);
	}

	@Logged
	@Post("/question/{id}/down")
	public void voteQuestionDown(Long id) {
		tryToVoteQuestion(id, VoteType.DOWN, Question.class);
	}

	@Logged
	@Post("/answer/{id}/up")
	public void voteAnswerUp(Long id) {
		tryToVoteQuestion(id, VoteType.UP, Answer.class);
	}

	@Logged
	@Post("/answer/{id}/down")
	public void voteAnswerDown(Long id) {
		tryToVoteQuestion(id, VoteType.DOWN, Answer.class);
	}

	private void tryToVoteQuestion(Long id, VoteType voteType, Class type) {
		Vote previous = votes.previousVoteFor(id, currentUser, type);
		Vote current = new Vote(currentUser, voteType);
				
		Votable votable = votes.loadVotedOnFor(type, id);
		votable.substitute(previous, current);
		votes.substitute(previous, current);
		result.nothing();
	}

}