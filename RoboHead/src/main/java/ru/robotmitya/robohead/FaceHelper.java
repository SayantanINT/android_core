package ru.robotmitya.robohead;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Выражение лица робота.
 * @author Дмитрий Дзахов
 *
 */
enum FaceType { ftUnknown, ftOk, ftHappy, ftBlue, ftAngry, ftIll, ftReadyToPlay };

/**
 * Класс для управления лицом робота.
 * @author Дмитрий Дзахов
 *
 */
public final class FaceHelper {
	/**
	 * Контрол ImageView в котором будет отображаться анимация.
	 */
	private ImageView mImageView;

	/**
	 * Последняя кадровая анимация.
	 */
	private AnimationDrawable mAnimation = null;
	
	/**
	 * Текущая мордочка.
	 */
	private FaceType mCurrentFace = FaceType.ftOk;
	
	/**
	 * Хэндлер, принимающий пустые сообщения, сигнализирующие о необходимости запустить автоматическую анимацию.
	 * Автоматическую, потому что она запускается не по команде, а сама, при простое робота. 
	 */
	private Handler mHandlerIdleAction = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			int resource;
			
			switch (mCurrentFace) {
			case ftOk:
				final int variansCount = 5;
				int choise = (int) Math.round((variansCount - 1) * Math.random());
				switch (choise) {
				case 0: // вероятность 20%
					resource = R.drawable.idle_action_ok_2;
					break;
				case 1: // вероятность 20%
					resource = R.drawable.idle_action_ok_3;
					break;
				default: // вероятность 60%
					resource = R.drawable.idle_action_ok_1;
					break;
				}				
				break;
			default:
				return;
			}
			
			startAnimation(resource);
		}
	};
	
	/**
	 * Хэндлер, принимающий сообщения, сигнализирующие о необходимости что-то сделать. 
	 */
//	private Handler mHandlerDelayedAction = new Handler() {
//		@Override
//		public void handleMessage(final Message msg) {
//			switch (mCurrentFace) {
//			case ftReadyToPlay:
//				setFace(FaceType.ftOk);
//				break;
//			default:
//				return;
//			}
//		}
//	};

	/**
	 * Конструктор класса.
	 * @param imageView контрол для вывода анимации.
	 */
	public FaceHelper(final ImageView imageView) {
		mImageView = imageView;
		
		// Thread для генерации Idle-событий. События генерируются с переменной периодичностью.
		// Промежутки составляют 4 сек + random(4 сек).
		new Thread() {
			public void run() {
				try {
					final int constDelay = 4000;
					final int variantMaxDelay = 4000;
					
					while (true) {					
						int variantDelay = (int) (variantMaxDelay * Math.random());
						Thread.sleep(constDelay + variantDelay);
						mHandlerIdleAction.sendEmptyMessage(0);
					}
				} catch (InterruptedException e) {
						e.printStackTrace();
				}
			}
		} .start();
	}
	
	/**
	 * Получение текущего состояния лица.
	 * @return тип установленного выражения лица.
	 */
	public FaceType getFace() {
		return mCurrentFace;
	}
	
	/**
	 * Установка выражения лица.
	 * @param face новое выражение лица.
	 * @return true если началась смена выражения лица, false если смена лица уже происходит.
	 */
	public boolean setFace(final FaceType face) {
		int resource = 0;

		switch (mCurrentFace) {
		case ftAngry:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_angry_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_angry_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_angry_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_angry_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_angry_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_angry_to_ready_to_play;
			} else {
				return false;
			}
			break;
		case ftBlue:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_blue_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_blue_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_blue_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_blue_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_blue_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_blue_to_ready_to_play;
			} else {
				return false;
			}
			break;
		case ftHappy:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_happy_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_happy_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_happy_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_happy_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_happy_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_happy_to_ready_to_play;
			} else {
				return false;
			}
			break;
		case ftIll:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_ill_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_ill_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_ill_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_ill_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_ill_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_ill_to_ready_to_play;
			} else {
				return false;
			}
			break;
		case ftReadyToPlay:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_ready_to_play_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_ready_to_play_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_ready_to_play_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_ready_to_play_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_ready_to_play_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_ready_to_play_to_ready_to_play;
			} else {
				return false;
			}
			break;
		case ftOk:
			if (face == FaceType.ftAngry) {
				resource = R.drawable.face_ok_to_angry;
			} else if (face == FaceType.ftBlue) {
				resource = R.drawable.face_ok_to_blue;
			} else if (face == FaceType.ftHappy) {
				resource = R.drawable.face_ok_to_happy;
			} else if (face == FaceType.ftIll) {
				resource = R.drawable.face_ok_to_ill;
			} else if (face == FaceType.ftOk) {
				resource = R.drawable.face_ok_to_ok;
			} else if (face == FaceType.ftReadyToPlay) {
				resource = R.drawable.face_ok_to_ready_to_play;
			} else {
				return false;
			}
			break;
		default:
			return false;
		}
		
		mCurrentFace = face;
		startAnimation(resource);
		
//		if (face == FaceType.ftReadyToPlay) {
//			final int readyToPlayPause = 5000;
//			mHandlerDelayedAction.sendEmptyMessageDelayed(0, readyToPlayPause);
//		}
		
        return true;
	}
	
	/**
	 * Запуск анимации.
	 * @param resource ресурс AnimationList.
	 */
	private void startAnimation(final int resource) {
		if (mAnimation != null) {
			mAnimation.stop();
			mAnimation = null;
			System.gc();
		}
		
		mImageView.setBackgroundResource(resource);
		mAnimation = (AnimationDrawable) mImageView.getBackground();
		mAnimation.start();
	}

    public static FaceType messageToFaceType(final String message) {
        String identifier = MessageHelper.getMessageIdentifier(message);
        if (!identifier.contentEquals(Rs.Mood.ID)) {
            return FaceType.ftUnknown;
        }

        int value = MessageHelper.getMessageIntegerValue(message);
        if (value == Rs.Mood.FACE_OK) {
            return FaceType.ftOk;
        }
        if (value == Rs.Mood.FACE_HAPPY) {
            return FaceType.ftHappy;
        }
        if (value == Rs.Mood.FACE_BLUE) {
            return FaceType.ftBlue;
        }
        if (value == Rs.Mood.FACE_ANGRY) {
            return FaceType.ftAngry;
        }
        if (value == Rs.Mood.FACE_ILL) {
            return FaceType.ftIll;
        }
        if (value == Rs.Mood.FACE_READY_TO_PLAY) {
            return FaceType.ftReadyToPlay;
        }
        if (value == Rs.Mood.ACTION_HAPPY) {
            return FaceType.ftHappy;
        }
        if (value == Rs.Mood.ACTION_PLAY) {
            return FaceType.ftReadyToPlay;
        }
        if (value == Rs.Mood.ACTION_SAD) {
            return FaceType.ftBlue;
        }
        if (value == Rs.Mood.ACTION_ANGRY) {
            return FaceType.ftAngry;
        }
        if (value == Rs.Mood.ACTION_DANCE) {
            return FaceType.ftHappy;
        }
        if (value == Rs.Mood.ACTION_NOSE) {
            return FaceType.ftOk;
        }
        return FaceType.ftUnknown;
    }
}
