package me.xtrm.meteorclient.antistaff;

import me.xtrm.meteorclient.antistaff.modules.AntiStaff;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AntiStaffAddon extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger(AntiStaffAddon.class);

	@Override
	public void onInitialize() {
		LOG.info("Initializing Meteor Addon Template");

		MeteorClient.EVENT_BUS.registerLambdaFactory(
            AntiStaffAddon.class.getPackageName(),
            (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

		Modules.get().add(new AntiStaff());
	}

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xtrm-en", "meteor-antistaff", "main");
    }

    @Override
    public String getWebsite() {
        var repo = getRepo();

        return "https://github.com/"
            + repo.getOwnerName()
            + "/tree/"
            + repo.branch();
    }
}
