package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/GoToPlayerPage")
public class GoToPlayerPage extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public GoToPlayerPage() {
		super();
	}

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		HttpSession session = request.getSession();
		String path = "/WEB-INF/Player.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		Integer song_ID = null;
		try {
			song_ID = Integer.parseInt(request.getParameter("song_ID"));
		} catch (NumberFormatException | NullPointerException e) {
			// only for debugging e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect songID value");
			return;
		}
		User user = (User) session.getAttribute("user");
		boolean validSongID=false;
		SongDAO songDAO= new SongDAO(connection);
		if(song_ID!=null) {
			
			List<Song> songsOfUser;
			try {
				songsOfUser = songDAO.findSongsByUserID(user.getId());
			} catch (SQLException e1) {
				e1.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Impossible to get user songs");
				return;
			}
			for(Song s: songsOfUser) {
				if(s.getSongID()==song_ID) {
					validSongID=true;
				}
			}
		}
		if(!validSongID) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect Song ID value");
			return;
		}
		Song song = null;
		try {
			song = songDAO.findSongById(song_ID);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover album");
		}
		if(song!=null) {
			ctx.setVariable("currentSong", song);
		}
		
		AlbumDAO albumDAO= new AlbumDAO(connection);
		Album album = null;
		if (song != null) {
			try {
				album = albumDAO.findAlbumBySongId(song.getSongID());
			} catch (SQLException | IOException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover album");
			}
		}
		ctx.setVariable("currentAlbum", album);
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}