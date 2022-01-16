package util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class IconHelper {
	public static Image ImageTransparentProduce(final int sizeX, final int sizeY) {
		try {
			BufferedImage bufferedImage = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = bufferedImage.createGraphics();
			// region 设置透明
			bufferedImage = graphics2D.getDeviceConfiguration().createCompatibleImage(sizeX, sizeY, Transparency.TRANSLUCENT);
			graphics2D = bufferedImage.createGraphics();
			// endregion
			graphics2D.drawRect(0, 0, sizeX - 1, sizeY - 1);
			return bufferedImage;
		} catch (Exception ignore) {
			return null;
		}
	}
}
