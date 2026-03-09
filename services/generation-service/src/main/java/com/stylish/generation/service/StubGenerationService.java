package com.stylish.generation.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class StubGenerationService {
	public byte[] generatePngPlaceholder(int size) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(22, 24, 29));
			g.fillRect(0, 0, size, size);

			g.setColor(new Color(255, 255, 255, 230));
			g.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, size / 18)));
			g.drawString("Stylish (stub)", size / 10, size / 3);

			g.setColor(new Color(255, 255, 255, 180));
			g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(14, size / 28)));
			g.drawString("Generated at: " + Instant.now(), size / 10, size / 3 + (size / 10));
		} finally {
			g.dispose();
		}

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to render placeholder image", e);
		}
	}
}

