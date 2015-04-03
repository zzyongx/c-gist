// Last updated 2012/03/08 11:15

// This implements the command:
// convert bunny_grass.gif ( bunny_anim.gif -repage 0x0+5+15! ) \
//            -coalesce -delete 0 -deconstruct -loop 0  bunny_bgnd.gif
// from Anthony's examples at: http://www.imagemagick.org/Usage/anim_basics/#cleared

#include <windows.h>
#include <wand/magick_wand.h>
void test_wand(void)
{
	MagickWand *mw = NULL;
	MagickWand *aw = NULL;
	MagickWand *tw = NULL;
	unsigned int i;

	MagickWandGenesis();

	/* Create a wand */
	mw = NewMagickWand();

	/* Read the first input image */
	if(MagickReadImage(mw,"bunny_grass.gif"));

//( bunny_anim.gif -repage 0x0+5+15\! )
	// We need a separate wand to do this bit in parentheses
	aw = NewMagickWand();
	if(MagickReadImage(aw,"bunny_anim.gif"));
	MagickResetImagePage(aw,"0x0+5+15!");

	// Now we have to add the images in the aw wand on to the end
	// of the mw wand.
	MagickAddImage(mw,aw);
	// We can now destroy the aw wand so that it can be used
	// for the next operation
	if(aw) aw = DestroyMagickWand(aw);

// -coalesce
	aw = MagickCoalesceImages(mw);

// do "-delete 0" by copying the images from the "aw" wand to
// the "mw" wand but omit the first one
	// free up the mw wand and recreate it for this step
	if(mw) mw = DestroyMagickWand(mw);
	mw = NewMagickWand();
	for(i=1;i<MagickGetNumberImages(aw);i++) {
		MagickSetIteratorIndex(aw,i);
		tw = MagickGetImage(aw);
		MagickAddImage(mw,tw);
		DestroyMagickWand(tw);
	}
	MagickResetIterator(mw);

	// free up aw for the next step
	if(aw) aw = DestroyMagickWand(aw);

// -deconstruct
// Anthony says that MagickDeconstructImages is equivalent
// to MagickCompareImagesLayers so we'll use that

	aw = MagickCompareImageLayers(mw,CompareAnyLayer);

// -loop 0
	MagickSetOption(aw,"loop","0");

	/* write the images into one file */
	if(MagickWriteImages(aw,"bunny_bgnd.gif",MagickTrue));

	/* Tidy up - note that the "tw" wand has already been destroyed */
	if(mw) mw = DestroyMagickWand(mw);
	if(aw) aw = DestroyMagickWand(aw);
	MagickWandTerminus();
}
