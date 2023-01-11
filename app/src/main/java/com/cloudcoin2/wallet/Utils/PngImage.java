package com.cloudcoin2.wallet.Utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkFactory;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkSingle;

/**
 * This class is used to manipulate chunks into PNG images for generating currency notes
 *
 *
 */
public class PngImage {

    // Example chunk: this stores a Java property as XML
    public static class PngChunkPROP extends PngChunkSingle {
        // ID must follow the PNG conventions: four ascii letters,
        // ID[0] : lowercase (ancillary)
        // ID[1] : lowercase if private, upppecase if public
        // ID[3] : uppercase if "safe to copy"
        public final static String ID = "cLDc";

        private byte[] coinData;



        // fill with your own "high level" properties, in this example,
        public PngChunkPROP(ImageInfo imgInfo) {
            super(ID, imgInfo);

        }

        public void setCoinData(byte[] data)
        {
          coinData = data;
        }

        @Override
        public ChunkRaw createRawChunk() {

            if(coinData.length==0)
                throw new PngjException("No Coin data to embed");
            /*
            // This code "serializes" your fields, according to our chunk spec
            // For other examples, see the code from other PngChunk implementations
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                props.storeToXML(bos, "MyChunk");
            } catch (IOException e) {
                throw new PngjException("error creating chunk", e);
            }*/
            ChunkRaw c = createEmptyChunk(coinData.length, true);
            System.arraycopy(coinData, 0, c.data, 0, c.len);
            raw = c;

            return c;
        }

        @Override
        public void parseFromRaw(ChunkRaw c) {
            /*
            // This code "deserializes" your fields, according to our chunk spec
            // For other examples, see the code from other PngChunk implementations
            props.clear();*/
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(c.data, 0, c.len);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[4];

                while ((nRead = bis.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
                byte[] byteData = buffer.toByteArray();
                coinData = byteData;
                //props.loadFromXML(bis);
            } catch (Exception e) {
                throw new PngjException("error creating chunk", e);
            }
        }

        @Override
        public ChunkOrderingConstraint getOrderingConstraint() {
            // change this if you don't require this chunk to be before IDAT, etc
            return ChunkOrderingConstraint.BEFORE_IDAT;
        }



    }

    public static void addPropChunk(InputStream orig, String dest, byte[] coinData) {
        if (orig.equals(dest))
            throw new RuntimeException("orig == dest???");
        //String temp = "src/test/output/temp.png";
        File myFile = new File(dest);
        if(myFile.exists())
            myFile.delete();


        PngReader pngr = new PngReader(orig);
        PngWriter pngw = new PngWriter(new File(dest), pngr.imgInfo, true);
        System.out.println("Reading : " + pngr.toString());
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
        PngChunkPROP mychunk = new PngChunkPROP(pngw.imgInfo);
        mychunk.setCoinData(coinData);
        mychunk.createRawChunk();
        mychunk.setPriority(true); // if we want it to be written as soon as possible
        pngw.getChunksList().queue(mychunk);
        for (int row = 0; row < pngr.imgInfo.rows; row++) {
            IImageLine l1 = pngr.readRow();
            pngw.writeRow(l1);
        }
        pngr.end();
        pngw.end();
        //AddText(temp,dest);
        System.out.printf("Done. Writen : " + dest);
    }


    public static void addPropChunk(String orig, String dest, byte[] coinData) {
        if (orig.equals(dest))
            throw new RuntimeException("orig == dest???");
        //String temp = "src/test/output/temp.png";
        File myFile = new File(dest);
        if(myFile.exists())
            myFile.delete();


        PngReader pngr = new PngReader(new File(orig));
        PngWriter pngw = new PngWriter(new File(dest), pngr.imgInfo, true);
        System.out.println("Reading : " + pngr.toString());
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
        PngChunkPROP mychunk = new PngChunkPROP(pngw.imgInfo);
        mychunk.setCoinData(coinData);

        mychunk.createRawChunk();
        mychunk.setPriority(true); // if we want it to be written as soon as possible
        pngw.getChunksList().queue(mychunk);
        for (int row = 0; row < pngr.imgInfo.rows; row++) {
            IImageLine l1 = pngr.readRow();
            pngw.writeRow(l1);
        }
        pngr.end();
        pngw.end();
        //AddText(temp,dest);
        System.out.printf("Done. Writen : " + dest);
    }

    static class MyCustomChunkFactory extends ChunkFactory { // this could also be an anonymous class
        @Override
        protected PngChunk createEmptyChunkExtended(String id, ImageInfo imgInfo) {
            if (id.equals(PngChunkPROP.ID))
                return new PngChunkPROP(imgInfo);
            return super.createEmptyChunkExtended(id, imgInfo);
        }
    }

    public static void readPropChunk(String ori) {
        // to read the "unkwnon" chunk as our desired chunk, we must statically register
        // it
        PngReader pngr = new PngReader(new File(ori));
        pngr.getChunkseq().setChunkFactory(new MyCustomChunkFactory());
        System.out.println("Reading : " + pngr.toString());
        pngr.readSkippingAllRows();
        pngr.end();
        // we know there can be at most one chunk of this type...
        PngChunk chunk = pngr.getChunksList().getById1(PngChunkPROP.ID);
        System.out.println(chunk);
        // the following would fail if we had not register the chunk
        PngChunkPROP chunkprop = (PngChunkPROP) chunk;
       // System.out.println(chunkprop != null ? chunkprop.getProps() : " NO PROP CHUNK");
    }

    public static void testRead() {
        readPropChunk("/temp/x2.png");
    }

}