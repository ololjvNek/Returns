-- Miesiąc ewidencji ma wynikać wyłącznie z kolumny Data zwrotu.
UPDATE return_record
SET period = date_trunc('month', return_date)::date
WHERE return_date IS NOT NULL
  AND period IS DISTINCT FROM date_trunc('month', return_date)::date;
