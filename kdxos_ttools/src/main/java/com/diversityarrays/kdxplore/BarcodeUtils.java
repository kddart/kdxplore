/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdxplore;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.pearcan.util.StringTemplate;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.core.Trait;
import com.diversityarrays.util.Pair;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.aztec.AztecWriter;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;

public class BarcodeUtils {


	/*
Instead, the barcode values for Traits are:
   '!TRAIT='<TraitId>'='<sentinel-char><actual-value><sentinel-char>

and those for a Trial Unit (plot) are:
   '!PLOT='<sentinel-char><TrialUnitId><sentinel-char>

Simple RE for recognition could be:

   ^ ! ( A+ ) = ( ( [ ^ = ] + ) = ) ( . ) ( [ ^ \4 ] * ) \4 $

where:
  Group 1== TRAIT or PLOT
  Group 3 == the Trait Id or empty for a TrialUnitId barcode
  Group 5 == the value; i.e. Trait value or TrialUnitId

Key points:
	 * Introducer (aka lead-in or prefix) character for entire barcode
   (May not need this but it may prove to be useful)
	 * Field separator is always '=' ; and 1 occurrence means TrialUnit, 2
means Trait
	 * Sentinel character - arbitrary so doesn't affect available chars for
the 'value'
	 * Trailing sentinel provides confirmation of end of the scan (in some
ways redundant because of actual barcode but is probably a good idea
anyway and some RE engines will perform better with it)
	 * No limit on available codes.
	 * No need for table nor lookup (& all the associated mgmt issues)
	 * Automatic detection of using the wrong "trait chart" (because the
TraitId doesn't match). This also means that data collection may be
easier - see below.
	 *** Allows for a convention to support 'infinite value ranges'

Easier data collection: (& no need for specifying "visit order")
At each plot:
- first scan the barcode that identifies the plot (Trial Unit in KDDart
parlance)
   KDSmart auto positions to the plot.
- then, for each Trait being measured, just scan the required value for
each Trait being measured. If you scan the wrong value, just scan the
chart again; the last value scanned for a given TraitId is taken as the
value to store. Note that this assumes only one value for each Trait -
is this assumption valid?

Large or Infinite Value Ranges
	 *=======================*
For a Trait that has a measurement scale of this form, the 'value' part
is left empty but KDSmart knows (from the TraitId and hence the
validation rule) that a simple "value entry" needs to occur.
The user then either manually enters the required value OR has a barcode
value chart where the barcodes are just the encoding of characters that
need to be entered (not of the special form above).
Normal (current) validation rule application follows.

There are some other implications that we should discuss in person.

Appendix for SeedPrepWizard
=======================
	 ***This isn't directly related to the KDX/S Trait value capture but I
want to get it down here so I don't forget it by the time I get back.

	 *While thinking on the barcode issue for KDX/S I was reminded of some
recent stuff that we were working on with the KDX SeedPrep work. We've
been flipping between a few constructions for barcode of the temporary
items that eventually get "promoted" to an Item when the upload is done.
We've been trying various options and haven't found something that I'm
completely happy with. But I'm now considering the following...

Requirements:
- as short as possible
- globally unique within the database instance
- can be generated while offline (hence no central generator/source)
	 ***- consider the possible need to use manual entry (for when someone is
doing seed prep and their barcode reader breaks down!)

	 *Use
    'I':<node-tiebreak>:<yearfrom2k><julianday>.<ms-tiebreak>

'I' for Item
node-tiebreak: use TrialId - assumption is that only one user will be
doing SeedPrep for any particular Trial.
yearfrom2k: offset from 2000 means only 2 digits
julianday: day in year is 3 digits (use zero fill)
ms-tiebreak: is millisecond plus "sequence within this millisecond" -
thus usually 3 to 5 digits (provided by the generator)

So far this adds up to: 1+1+n+1+2+3+5 = 13+n digits*

To cater for manual entry we probably want to add a check digit and
perhaps use a variant of z-base-32 to improve the human-engineering aspect.
	 */
	
    private static final String ERRMSG_REQUESTED_ROW_OUTSIDE_IMAGE = 
            "Requested row is outside the image: {0,number,integer}"; //$NON-NLS-1$
    

	static public Result scanBarcode(BufferedImage image) throws NotFoundException {
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		
		BinaryBitmap bb = new BinaryBitmap(new  HybridBinarizer(source));
		
		MultiFormatReader mfr = new MultiFormatReader();
		return mfr.decode(bb);
	}

	static public List<Pair<String, BufferedImage>> barcodesForTrait(BarcodeFormat barcodeFormat, Trait trait) throws InvalidRuleException {
		String traitValRule = trait.getTraitValRule();
		ValidationRule rule = ValidationRule.create(traitValRule);
		try {
			return barcodesForRule(barcodeFormat, 
					trait.getTraitId(), 
					traitValRule);
		} catch (UnsupportedOperationException e) {
			throw new InvalidRuleException("Unsupported ValidationRuleType: " +  //$NON-NLS-1$
			        rule.getValidationRuleType());
		}
	}

	static private final String  TRAIT_template = "TRAIT:${traitId}:${traitValue}"; //$NON-NLS-1$
	//	static private final String  TRAIT_template = "TRAIT:${traitId}:|${traitValue}|";


	public static List<Pair<String, BufferedImage>> barcodesForRule(
			BarcodeFormat barcodeFormat,
			Integer traitId, 
			String traitValRule)
	{
		List<Pair<String,BufferedImage>> imagesByValue = new ArrayList<Pair<String,BufferedImage>>();

		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

		try {
//			ValidationRule rule = ValidationRule.createForChecking(traitValRule,
//					TraitDataType.ELAPSED_DAYS == traitDataType);
			ValidationRule rule = ValidationRule.create(traitValRule);

			for (String value : rule) {
				String barcode = StringTemplate.buildString(TRAIT_template)
				        .replace("traitId", traitId) //$NON-NLS-1$
						.replace("traitValue", value) //$NON-NLS-1$
						.build();
				System.out.println(barcode);

				BufferedImage img = createOneBarcodeImage(barcodeFormat, barcode, barcode, font, 200, 80);
				imagesByValue.add(new Pair<String,BufferedImage>(value, img));
			}

		} catch (InvalidRuleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return imagesByValue;
	}

	// TODO change this to take a Barcode format parameter (com.google.zxing.BarcodeFormat)
	// We need to be able to barcode the characters that the BarcodeGenerator creates.
	// and I am proposing that it will use 
	
	// Support for:
	//  2D: AZTEC, QR_CODE, DATA_MATRIX
	//  1D: CODE_128, PDF_417 (actually "stacked linear")
	//
	// Don't use:
	// CODE_39: too few supported chars (we need full ascii to support Categorical choices)
	// CODE_93: although it does support full ascii 
	// 
	// Check on 1D: EAN_13, EAN_8, ITF, UPC_A, UPC_E
	//          2D: MAXICODE
	// ?? RSS_14, RSS_EXPANDED, UPC_EAN
	static public BufferedImage createOneBarcodeImage(
			BarcodeFormat barcodeFormat,
			String barcode, 
			String label, 
			Font font, 
			int barcodeWidth, 
			int imageHeight) {

		Graphics2D g = null;
		try {

			BufferedImage img = new BufferedImage(barcodeWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

			g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


			FontMetrics fm = g.getFontMetrics(font);
			final int textHeight    = fm.getMaxAscent() + fm.getMaxDescent();

			BufferedImage barcodeImage = createBarcodeImage(barcodeFormat,
					barcode, barcodeWidth, imageHeight - textHeight);

			g.fillRect(0,0, barcodeWidth, imageHeight);

			g.drawImage(barcodeImage, 0, 0, null);

			g.setFont(font);
			if (label != null) {
				int swid = g.getFontMetrics(g.getFont()).stringWidth(label);
				g.setColor(Color.BLACK);

				g.drawString(label, Math.max(0, (barcodeWidth - swid)/2), imageHeight - (textHeight / 2) + fm.getDescent());
			}
			g.dispose();

			return img;

		} catch (WriterException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (g != null) {
				g.dispose();
			}
		}
	}
	
	/**
	 * @param barcodeFormat
	 * @param barcode
	 * @param barcodeWidth
	 * @param barcodeHeight
	 * @return
	 * @throws IllegalArgumentException
	 * @throws WriterException
	 * @throws IOException
	 */
	static public BufferedImage createBarcodeImage(
				BarcodeFormat barcodeFormat,
				String barcode, 
				int barcodeWidth,
				int barcodeHeight)
	throws IllegalArgumentException, WriterException, IOException 
	{
		Writer writer = createBarcodeWriter(barcodeFormat);
		
		if (writer == null) {
			throw new IllegalArgumentException(barcodeFormat.name());
		}
		
		if (barcode == null) {
			return null;
		}
		
		BitMatrix encoded = writer.encode(barcode, barcodeFormat, barcodeWidth, barcodeHeight);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(encoded, "png", os); //$NON-NLS-1$
		os.close();
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
		return img;
	}

	public static Writer createBarcodeWriter(BarcodeFormat barcodeFormat) {
		Writer writer = null;
		switch (barcodeFormat) {
		case AZTEC:
			writer = new AztecWriter();
			break;
		case CODABAR:
			break;
		case CODE_128:
			writer = new Code128Writer();
			break;
		case CODE_39: // Only supports A-Z 0-9 $ / + - % . , SPACE
			//writer = new Code39Writer(); // Won't support some chars: Bad contents: TR/22cNsoy5rG9
			break;
		case CODE_93: // Only supports A-Z 0-9 $ / + - % .SPACE
			// writer = new MultiFormatWriter(); // No encoder available for format CODE_93
			break;
		case DATA_MATRIX: // 2D
			writer = new DataMatrixWriter(); 
			// Can't find a symbol arrangement that matches the message. Data codewords: 13
			break;
		case EAN_13:
			break;
		case EAN_8:
			break;
		case ITF:
			break;
		case MAXICODE:
			break;
		case PDF_417:
			writer = new PDF417Writer(); // stacked linear (1D-ish)
			break;
		case QR_CODE:
			writer = new QRCodeWriter();
			break;
		case RSS_14:
			break;
		case RSS_EXPANDED:
			break;
		case UPC_A:
			break;
		case UPC_E:
			break;
		case UPC_EAN_EXTENSION:
			break;
		default:
			break;
		}
		return writer;
	}

//	static public BufferedImage makeCodeX(String barcode, int barcodeWidth,
//			int barcodeHeight) throws WriterException, IOException
//	{
//		Code39Writer w = new Code39Writer();
//		BitMatrix encoded = w.encode(barcode, BarcodeFormat.CODE_39, barcodeWidth, barcodeHeight);
//		
//		return buildImageFromBitMatrix(encoded);
//	}
//	
//	static public BufferedImage makeCode128(String barcode, int barcodeWidth,
//			int barcodeHeight) throws WriterException, IOException 
//	{
//		Code128Writer w = new Code128Writer();
//		BitMatrix encoded = w.encode(barcode, BarcodeFormat.CODE_128, barcodeWidth, barcodeHeight, null);
//		
//		return buildImageFromBitMatrix(encoded);
//	}
//
//	public static BufferedImage buildImageFromBitMatrix(BitMatrix encoded) 
//	throws IOException 
//	{
//		ByteArrayOutputStream os = new ByteArrayOutputStream();
//		MatrixToImageWriter.writeToStream(encoded, "png", os);
//		os.close();
//		BufferedImage img = ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
//		return img;
//	}

	static public Result decodeBitmapFromImageData(BufferedImage image) throws NotFoundException {
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		return new MultiFormatReader().decode(bitmap, null);
	}
	
	static public Result decodeBitmapFromImageData(byte[] image /*, int width, int height */) 
	throws NotFoundException, IOException {
		LuminanceSource source = BitmapLuminanceSource.create(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		return new MultiFormatReader().decode(bitmap, null);
	}
	
    static class BitmapLuminanceSource extends LuminanceSource {
    	
    	static public BitmapLuminanceSource create(byte[] jpegbytes) throws IOException {
    		BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpegbytes));

    		int w = img.getWidth();
    		int h = img.getHeight();
			int[] rgbArray = img.getRGB(0, 0, w, h, null, 0, w);
    		return new BitmapLuminanceSource(rgbArray, w, h);
    	}

        private final byte[] luminances;
        private final int dataWidth;
        private final int dataHeight;
        private final int left;
        private final int top;

        public BitmapLuminanceSource(int[] pixels, int w, int h) {
            super(w, h);

            dataWidth = super.getWidth();
            dataHeight = super.getHeight();
            left = 0;
            top = 0;

            // In order to measure pure decoding speed, we convert the entire image to a greyscale array
            // up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
            luminances = new byte[dataWidth * dataHeight];
            for (int y = 0; y < dataHeight; y++) {
                int offset = y * dataWidth;
                for (int x = 0; x < dataWidth; x++) {
                    int pixel = pixels[offset + x];
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    if (r == g && g == b) {
                        // Image is already greyscale, so pick any channel.
                        luminances[offset + x] = (byte) r;
                    } else {
                        // Calculate luminance cheaply, favoring green.
                        luminances[offset + x] = (byte) ((r + 2 * g + b) / 4);
                    }
                }
            }
        }


        @Override
        public byte[] getRow(int y, byte[] row) {
            if (y < 0 || y >= getHeight()) {
                throw new IllegalArgumentException(MessageFormat.format(ERRMSG_REQUESTED_ROW_OUTSIDE_IMAGE, y));
            }
            int width = getWidth();
            if (row == null || row.length < width) {
                row = new byte[width];
            }
            int offset = (y + top) * dataWidth + left;
            System.arraycopy(luminances, offset, row, 0, width);
            return row;
        }

        @Override
        public byte[] getMatrix() {
            int width = getWidth();
            int height = getHeight();

            // If the caller asks for the entire underlying image, save the copy and give them the
            // original data. The docs specifically warn that result.length must be ignored.
            if (width == dataWidth && height == dataHeight) {
                return luminances;
            }

            int area = width * height;
            byte[] matrix = new byte[area];
            int inputOffset = top * dataWidth + left;

            // If the width matches the full width of the underlying data, perform a single copy.
            if (width == dataWidth) {
                System.arraycopy(luminances, inputOffset, matrix, 0, area);
                return matrix;
            }

            // Otherwise copy one cropped row at a time.
            byte[] rgb = luminances;
            for (int y = 0; y < height; y++) {
                int outputOffset = y * width;
                System.arraycopy(rgb, inputOffset, matrix, outputOffset, width);
                inputOffset += dataWidth;
            }
            return matrix;
        }
    }


	static class JpegByteArrayLuminanceSource extends LuminanceSource {


        private final byte[] luminances;
		private final int dataWidth;
		private final int dataHeight;
		private final int left;
		private final int top;

		public JpegByteArrayLuminanceSource(byte[] pixels, int width, int height) {
			super(width, height);

			dataWidth = width;
			dataHeight = height;
			left = 0;
			top = 0;

			// In order to measure pure decoding speed, we convert the entire image to a greyscale array
			// up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
			luminances = new byte[width * height];
			for (int y = 0; y < height; y++) {
				int offset = y * width;
				for (int x = 0; x < width; x++) {
					int pixel = pixels[offset + x];
					int r = (pixel >> 16) & 0xff;
					int g = (pixel >> 8) & 0xff;
					int b = pixel & 0xff;
					if (r == g && g == b) {
						// Image is already greyscale, so pick any channel.
						luminances[offset + x] = (byte) r;
					} else {
						// Calculate luminance cheaply, favoring green.
						luminances[offset + x] = (byte) ((r + 2 * g + b) / 4);
					}
				}
			}

		}

		private JpegByteArrayLuminanceSource(byte[] pixels,
				int dataWidth,
				int dataHeight,
				int left,
				int top,
				int width,
				int height) {
			super(width, height);
			if (left + width > dataWidth || top + height > dataHeight) {
				throw new IllegalArgumentException("Crop rectangle does not fit within image data."); //$NON-NLS-1$
			}
			this.luminances = pixels;
			this.dataWidth = dataWidth;
			this.dataHeight = dataHeight;
			this.left = left;
			this.top = top;
		}


		@Override
		public byte[] getMatrix() {
			int width = getWidth();
			int height = getHeight();

			// If the caller asks for the entire underlying image, save the copy and give them the
			// original data. The docs specifically warn that result.length must be ignored.
			if (width == dataWidth && height == dataHeight) {
				return luminances;
			}

			int area = width * height;
			byte[] matrix = new byte[area];
			int inputOffset = top * dataWidth + left;

			// If the width matches the full width of the underlying data, perform a single copy.
			if (width == dataWidth) {
				System.arraycopy(luminances, inputOffset, matrix, 0, area);
				return matrix;
			}

			// Otherwise copy one cropped row at a time.
			byte[] rgb = luminances;
			for (int y = 0; y < height; y++) {
				int outputOffset = y * width;
				System.arraycopy(rgb, inputOffset, matrix, outputOffset, width);
				inputOffset += dataWidth;
			}
			return matrix;
		}

		@Override
		public byte[] getRow(int y, byte[] row) {
			if (y < 0 || y >= getHeight()) {
				throw new IllegalArgumentException(MessageFormat.format(ERRMSG_REQUESTED_ROW_OUTSIDE_IMAGE, y));
			}
			int width = getWidth();
			if (row == null || row.length < width) {
				row = new byte[width];
			}
			int offset = (y + top) * dataWidth + left;
			System.arraycopy(luminances, offset, row, 0, width);
			return row;
		}

		@Override
		  public boolean isCropSupported() {
		    return true;
		  }

		  @Override
		  public LuminanceSource crop(int left, int top, int width, int height) {
		    return new JpegByteArrayLuminanceSource(luminances,
		                                  dataWidth,
		                                  dataHeight,
		                                  this.left + left,
		                                  this.top + top,
		                                  width,
		                                  height);
		  }


	}
}
