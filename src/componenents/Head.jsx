import { Helmet } from 'react-helmet';

function Head() {
  return (
    <Helmet>
      <html lang="en" />
      <title>Flybot</title>
      <meta name="description" content="Flybot Pte Ltd is a software company based in Singapore specialized in Clojure developement." />
    </Helmet>
  )
}

export default Head