-- Misc Utility functions

{-# OPTIONS -fglasgow-exts #-}
module PredictorTestSuite.Util
  ( uncurry3
  , uncurry4
  , uncurry5
  , deletes
  , backspace
  , doNTimes
  ) where

  import Data.List
  import System.IO

  uncurry3 :: (a -> b -> c -> d) -> (a, b, c) -> d
  uncurry3 f = \(a, b, c) -> f a b c

  uncurry4 :: (a -> b -> c -> d -> e) -> (a, b, c, d) -> e
  uncurry4 f = \(a, b, c, d) -> f a b c d

  uncurry5 :: (a -> b -> c -> d -> e -> f) -> (a, b, c, d, e) -> f
  uncurry5 f = \(a, b, c, d, e) -> f a b c d e


  -- For every element of the first argument, delete the first occurance of that element in the second
  deletes :: Eq a => [a] -> [a] -> [a]
  deletes (l:ls) rs = deletes ls $ l `delete` rs
  deletes [] rs = rs

  -- Do an IO action n times
  doNTimes :: Int -> IO () -> IO ()
  doNTimes n = sequence_ . replicate n

  -- Backspace n times
  backspace :: Int -> IO ()
  backspace n = doNTimes n $ putStr "\^H"

  -- The verbose monad. Structure computations into ShowVerbose or NoShow. The semantics are that ShowVerbose
  -- computations will printout their associated IO Strings, while NoShow will merely propegate the io actions,
  -- and perform none of its own. Verbose is lazy, and thus at the end of the pipeline
  -- you end up with a series of built up IO actions. todo: add some-verbose-exiting-function, possibly make
  -- Verbose Handle-independent
  data Verbose a = ShowVerbose (IO String, a) | NoShow (IO String, a)

  instance Monad Verbose where
    return a = NoShow (return "", a)
    (ShowVerbose (s,a)) >>= g = case g a of ShowVerbose (s', a') -> ShowVerbose (((s >>= putStrLn) >> s'), a')
                                            NoShow (s',a') -> NoShow (((s >>= putStrLn) >> s'), a')
    (NoShow (act, a)) >>= g = case g a of ShowVerbose (s', a') -> ShowVerbose ((act >> s'), a')
                                          NoShow (s',a') -> NoShow ((act >> s'), a')

  -- flush the built up verbose actions
  flushVerbose :: Verbose a -> IO ()
  flushVerbose (ShowVerbose (s, a)) = s >> hFlush stdout
  flushVerbose (NoShow (s, a))      = s >> hFlush stdout

  -- run everything, print out what's verbose, and return the contained item (wrapped in IO)
  execVerbose :: Verbose a -> IO a
  execVerbose v@(ShowVerbose (s, a)) = flushVerbose v >> return a
  execVerbose v@(NoShow (s, a))      = flushVerbose v >> return a

  -- The rest of the file is examples and tests


  add1V n = NoShow (return "don't print me", n+1)

  ex1 = ShowVerbose (return "print me", 5) >>= add1V

  ret = ex1 >>= add1V >>= add1V

  retS = do putStrLn "starting"
            case ret of ShowVerbose (s,v) -> (s >>= putStrLn) >> (putStrLn (show v))
                        NoShow (s,v) -> s >> (putStrLn (show v))
