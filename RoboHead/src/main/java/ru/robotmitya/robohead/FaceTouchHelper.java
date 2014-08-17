package ru.robotmitya.robohead;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import ru.robotmitya.robocommonlib.Log;

/**
 * Класс для контроля касаний к лица робота.
 * @author Дмитрий Дзахов
 *
 */
public final class FaceTouchHelper implements OnTouchListener {
    private Context mContext;

	/**
	 * Менеджер управления лицом.
	 */
	private FaceHelper mFaceHelper;

    private ImageView mImageView;

	/**
	 * Сохранённая x-координата экрана в момент касания.
	 */
	private float mLastUniX;

	/**
	 * Сохранённая x-координата экрана в момент касания.
	 */
	//private float mLastUniY;

	/**
	 * Признак наличия тактильного контакта пользователя с экраном.
	 */
	private boolean mInTouch;

	/**
	 * Коэффициенты перевода координат касания расчитываются один раз при первом касании.
	 * Признак используется для определения первого касания.
	 */
	private boolean mCoefsCalculated = false;
	
	/**
	 * Коэффициент для перевода X-координаты касания в универсальные координаты.
	 * Универсальные координаты касания определены в диапазоне от 0 до 1.
	 */
	private float mCoefX;
	
	/**
	 * Коэффициент для перевода Y-координаты касания в универсальные координаты.
	 * Универсальные координаты касания определены в диапазоне от 0 до 1.
	 */
	private float mCoefY;
	
	/**
	 * Суммарная длина поглаживания.
	 */
	private float mStrokeSize;
	
	/**
	 * Конструктор класса.
	 * @param imageView контрол для вывода анимации.
	 * @param faceHelper менеджер управления лицом.
	 */
	public FaceTouchHelper(final Context context, final ImageView imageView, final FaceHelper faceHelper) {
        mContext = context;
		mFaceHelper = faceHelper;
        mImageView = imageView;
		imageView.setOnTouchListener(this);
	}

	/**
	 * Обработчик косаний лица.
	 * @param v источник касания.
	 * @param event сработавшее событие.
	 * @return true, если действие распознано и обработано.
	 */
	public boolean onTouch(final View v, final MotionEvent event) {
        if (!mCoefsCalculated) {
            mCoefX = 1f / (float)mImageView.getWidth();
            mCoefY = 1f / (float)mImageView.getHeight();
            mCoefsCalculated = true;
        }

		float uniX = getUniX(event.getX());
		float uniY = getUniY(event.getY());

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (isInHairArea(uniX, uniY)) {
				mLastUniX = uniX;
				mInTouch = true;
			} else if (isNoseArea(uniX, uniY)) {
				pushNose();
			} else if (isEyeArea(uniX, uniY)) {
				pushEye();
			}
			break;
		case MotionEvent.ACTION_UP:
			mInTouch = false;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mInTouch) {
				mStrokeSize += Math.abs(uniX - mLastUniX);
				mLastUniX = uniX;
				final float maxStrokeSize = 0.35f;
				if (mStrokeSize > maxStrokeSize) {
					mStrokeSize = 0;
					mInTouch = false;

                    patHair();
				}
			} else {
				mInTouch = false;
			}
			break;
		default:
			break;
		}
		
		return true;
	}
	
	/**
	 * Преобразование x-координаты касания в универсальную координату.
	 * @param x координата.
	 * @return универсальную x-координату.
	 */
	private float getUniX(final float x) {
		if (mCoefsCalculated) {
			return mCoefX * x;
		} else {
			return 0;
		}
	}
	
	/**
	 * Преобразование y-координаты касания в универсальную координату.
	 * @param y координата.
	 * @return универсальную y-координату.
	 */
	private float getUniY(final float y) {
		if (mCoefsCalculated) {
			return mCoefY * y;
		} else {
			return 0;
		}
	}
	
	/**
	 * Определение зоны поглаживания.
     * @param uniX координата.
	 * @param uniY координата.
	 * @return true, если координаты соответствуют зоне поглаживания.
	 */
	private boolean isInHairArea(final float uniX, final float uniY) {
        if (isEyeArea(uniX, uniY)) {
            return false;
        }

		final float foreHeadMaxY = 0.180f;
		return (mFaceHelper.getFace() == FaceType.ftOk) && (uniY < foreHeadMaxY);		
	}
	
	/**
	 * Определение зоны носа.
	 * @param uniX координата.
	 * @param uniY координата.
	 * @return true, если координаты соответствуют зоне носа.
	 */
	private boolean isNoseArea(final float uniX, final float uniY) {
		final float minX = 0.4f;
		final float maxX = 0.6f;
		final float minY = 0.4f;
		final float maxY = 0.6f;
		
		return (uniX >= minX) && (uniX < maxX) && (uniY >= minY) && (uniY < maxY);
	}
	
	/**
	 * Действие при нажатии на нос.
	 */
	private void pushNose() {
        Log.d(this, "nose was pushed");
        Intent intent = new Intent(FaceNode.BROADCAST_FACE_PUSH_NOSE);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	/**
	 * Определение зоны глаз.
	 * @param uniX координата.
	 * @param uniY координата.
	 * @return true, если координаты соответствуют зоне глаз.
	 */
	private boolean isEyeArea(final float uniX, final float uniY) {
		// Левый глаз:
		final float minLX = 0.225f;
		final float maxLX = 0.360f;
		final float minLY = 0.132f;
		final float maxLY = 0.370f;
		
		// Правый глаз:
		final float minRX = 0.618f;
		final float maxRX = 0.817f;
		final float minRY = 0.067f;
		final float maxRY = 0.420f;

		boolean isLeftEyeArea = (uniX >= minLX) && (uniX < maxLX) && (uniY >= minLY) && (uniY < maxLY);
		boolean isRightEyeArea = (uniX >= minRX) && (uniX < maxRX) && (uniY >= minRY) && (uniY < maxRY);
		return (mFaceHelper.getFace() == FaceType.ftOk) && (isLeftEyeArea || isRightEyeArea);
	}
	
	/**
	 * Действие при тычке в глаз.
	 */
	private void pushEye() {
        Log.d(this, "eye was pushed");
        Intent intent = new Intent(FaceNode.BROADCAST_FACE_PUSH_EYE);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

    private void patHair() {
        Log.d(this, "hair patting");
        Intent intent = new Intent(FaceNode.BROADCAST_FACE_PATTING);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
