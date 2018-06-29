package io.anuke.mindustry;

import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ModuleCore {
	private AsyncExecutor exec = new AsyncExecutor(1);

	@Override
	public void init(){
		Timers.mark();

		Vars.init();

		debug = Platform.instance.isDebug();

		Log.setUseColors(false);
		BundleLoader.load();
		ContentLoader.load();

		module(logic = new Logic());
		module(world = new World());
		module(control = new Control());
		module(renderer = new Renderer());
		module(ui = new UI());
		module(netServer = new NetServer());
		module(netClient = new NetClient());

        Log.info("Time to load [total]: {0}", Timers.elapsed());
	}

	@Override
	public void render(){
		super.render();
		threads.handleRender();
	}

}
