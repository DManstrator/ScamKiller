package tk.dmanstrator.scamkiller;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import tk.dmanstrator.scamkiller.listener.BotListener;
import tk.dmanstrator.scamkiller.utils.Configuration;

public class ScamKiller {
	private final JDA jda;
	private final Configuration config = new Configuration("config.json");
	
	public ScamKiller() throws LoginException  {
		jda = new JDABuilder(config.getToken())
				.addEventListeners(new BotListener())
				.setActivity(Activity.playing("Killing Scammers"))
				.build();
	}
	
	public JDA getJDA() {
		return jda;
	}
	
	public static void main(String[] args) {
		try {
			new ScamKiller();
		} catch (LoginException e) {
			e.printStackTrace();
		}

	}

}
