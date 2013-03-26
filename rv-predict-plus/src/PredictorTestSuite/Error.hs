-- Error handling (a Either-based monad)

module PredictorTestSuite.Error where

  data Result a = Result a | Error String
    deriving (Show, Eq, Read)

  -- Propagate errors, otherwise keep the results flowing
  instance Monad Result where
    return = Result
    Error s >>= _ = Error s
    Result a >>= f = f a
