
import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.primitives.MutableFloat;

import java.util.Locale;


public class Main {
    public static void main(String[] args) {
		// Demo

//		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
//		cfg.resizable = false;
//		cfg.vSyncEnabled = true;
//		cfg.useGL20 = false;
//		cfg.width = 800;
//		cfg.height = 480;
//		cfg.title = "Tween-Engine tests";
//		new LwjglApplication(new App(), cfg);

		// Tests

		float step = 0.0001f;

		System.out.println("-----------------------------------------------");
		System.out.println("Tween (v:value, lt:localTime, gt:globalTime)");
		System.out.println("-----------------------------------------------");
		testTween(step);

		System.out.println("-----------------------------------------------");
		System.out.println("Timeline (v:value, lt:localTime, gt:globalTime)");
		System.out.println("-----------------------------------------------");
		testTimeline(step);
	}

	private static void testTween(float step) {
		MutableFloat target = new MutableFloat(0);
        TweenCallback t1 = buildCallback("t", target);
        t1.setTriggers(TweenCallback.Events.ANY);

        Tween t = Tween.to(target, 0, 1.0f)
                       .target(1)
                       .repeat(2, 1)
                       .delay(1)
                       .addCallback(t1)
                       .start();

		float acc = 0;
		while (acc < t.getFullDuration()+1) {
			t.update(step);
			acc += step;
		}
		System.out.println("-----------------------------------------------");
		while (acc > -1) {
			t.update(-step);
			acc -= step;
		}
		System.out.println("-----------------------------------------------");
		while (acc < t.getFullDuration()+1) {
			t.update(step);
			acc += step;
		}
	}

	private static void testTimeline(float step) {
		MutableFloat target1 = new MutableFloat(0);
		MutableFloat target2 = new MutableFloat(0);
		MutableFloat target3 = new MutableFloat(0);
		Tween t1 = Tween.call(buildCallback("t1"));
		Tween t2 = Tween.call(buildCallback("t2"));
		Tween t3 = Tween.call(buildCallback("t3"));

        TweenCallback tl1 = buildCallback("TL");
        tl1.setTriggers(TweenCallback.Events.ANY);
        Timeline tl = Timeline.createSequence()
                              .push(t1)
                              .pushPause(1)
                              .push(t2)
                              .pushPause(1)
                              .push(t3)
                              .repeat(2, 1)
                              .addCallback(tl1)
                              .start();

		float acc = 0;
		while (acc < tl.getFullDuration()+1) {
			tl.update(step);
			acc += step;
		}
		System.out.println("-----------------------------------------------");
		while (acc > -1) {
			tl.update(-step);
			acc -= step;
		}
		System.out.println("-----------------------------------------------");
		while (acc < tl.getFullDuration()+1) {
			tl.update(step);
			acc += step;
		}
	}

	private static TweenCallback buildCallback(final String name, final MutableFloat target) {
		return new TweenCallback() {
			@Override public void onEvent(int type, BaseTween<?> source) {
				String t = type == TweenCallback.Events.BEGIN ? "BEGIN        "
					: type == TweenCallback.Events.START ? "START        "
					: type == TweenCallback.Events.END ? "END          "
					: type == TweenCallback.Events.COMPLETE ? "COMPLETE     "
					: type == TweenCallback.Events.BACK_BEGIN ? "BACK_BEGIN   "
					: type == TweenCallback.Events.BACK_START ? "BACK_START   "
					: type == TweenCallback.Events.BACK_END ? "BACK_END     "
					: type == TweenCallback.Events.BACK_COMPLETE ? "BACK_COMPLETE"
					: "???";

				String str = String.format(Locale.US, "%s %s   lt %.2f   v %.2f", name, t, source.getCurrentTime(), target.floatValue());
				System.out.println(str);
			}
		};
	}

	private static TweenCallback buildCallback(final String name) {
		return new TweenCallback() {
			@Override public void onEvent(int type, BaseTween<?> source) {
				String t = type == TweenCallback.Events.BEGIN ? "BEGIN        "
					: type == TweenCallback.Events.START ? "START        "
					: type == TweenCallback.Events.END ? "END          "
					: type == TweenCallback.Events.COMPLETE ? "COMPLETE     "
					: type == TweenCallback.Events.BACK_BEGIN ? "BACK_BEGIN   "
					: type == TweenCallback.Events.BACK_START ? "BACK_START   "
					: type == TweenCallback.Events.BACK_END ? "BACK_END     "
					: type == TweenCallback.Events.BACK_COMPLETE ? "BACK_COMPLETE"
					: "???";

				String str = String.format(Locale.US, "%s %s   lt %.2f", name, t, source.getCurrentTime());
				System.out.println(str);
			}
		};
	}
}
